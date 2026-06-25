# Design — spring-ai-agent v2 (behavior + contracts)

Governed by `constitution.md`.

## A. SSE streaming
### Wire contract — `POST /api/chat/stream` (Accept: text/event-stream)
Request: same body as `/api/chat` (`{message, conversationId?}`).
Events:
```
event: token
data: {"text":"It"}
event: token
data: {"text":"'s 14"}
...
event: done
data: {"reply":"<full>","toolsUsed":["weather"],"conversationId":"<uuid>"}
```
On failure:
```
event: error
data: {"error":"The agent could not complete your request (upstream error)."}
```
### UX states (chat UI)
| State | Render |
|-------|--------|
| sending | user bubble + assistant bubble with "using tools…" dots |
| streaming | assistant bubble fills token-by-token; caret cursor |
| done | tool chips appear under the completed bubble |
| error | red-bordered bubble with the error text + retry |

### toolsUsed in stream (R1/R2)
Tools run before text streams. UI shows "using tools…" until the first `token`. Final `toolsUsed` arrives only in `done`. Capture is per-call (ToolCallSink), never ThreadLocal.

## B. Persistent memory
- Table `chat_message(conversation_id TEXT, seq BIGINT, role TEXT, content TEXT, created_at TIMESTAMPTZ)`, PK `(conversation_id, seq)`.
- `append`: next seq per conversation; after insert, delete rows beyond the last `maxMessages` for that conversation (bounded). 
- `history`: `SELECT ... WHERE conversation_id=? ORDER BY seq DESC LIMIT maxMessages` then reverse → oldest-first `List<Message>` (role→UserMessage/AssistantMessage).
- Behavior parity with in-memory store asserted by the SAME test contract run against both impls.

## C. Rate limit + metrics
- Limit (configurable; defaults): capacity 20, refill 20 / minute, per IP. Response 429 `{"error":"Rate limit exceeded. Try again shortly."}` + `Retry-After: <seconds>`.
- Excluded paths: `/actuator/**`, static assets. Limited: `/api/chat`, `/api/chat/stream`.
- Metrics names: `agent.chat.requests` (counter), `agent.tool.invocations` (counter, tag `tool`). Scrapeable at `/actuator/prometheus`.

## D. MCP server
- HTTP/SSE: tools list returns 4 tools (`calculate`,`currentTime`,`weather`,`fetchUrl`) with the same `@Tool` descriptions; calling `calculate {"expression":"2+2"}` returns `4` — parity with in-app.
- stdio: `java -Dspring.profiles.active=mcp-stdio -jar agent.jar` (web off, logs→stderr). Documented for Claude Desktop config.

## Eval cases (runtime verification)
1. Stream "weather in Paris" → tokens arrive incrementally; `done` has `toolsUsed:[weather]`; row count in Postgres +2.
2. Stream a pure chat → tokens, `toolsUsed:[]`.
3. 21 rapid requests from one IP (cap 20) → ≥1 `429` with `Retry-After`.
4. Two IPs each under cap → both `200`.
5. `curl /actuator/prometheus` → contains `agent_chat_requests_total` and `agent_tool_invocations_total`.
6. MCP inspector lists 4 tools; calling calculator returns 4.
7. Restart app (Postgres profile) → prior conversation history still returned (persistence survives restart).

## Reviewer pass (pre-implementation)
- ✅ ThreadLocal-vs-stream addressed by a dedicated sink + a test that fails under ThreadLocal.
- ✅ Memory parity enforced by one contract test over both impls — no drift.
- ⚠ stdio MCP clean-stdout is the riskiest unknown (R3) — isolate in its own profile + a smoke check; don't block HTTP MCP on it.
- ⚠ Streaming + tool-calling chunk interleaving — if `stream().content()` mishandles tool loops, fall back to `chatClientResponse()` + aggregator (noted in plan A-T2).
