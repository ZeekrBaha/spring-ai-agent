# Architecture — spring-ai-agent v2

Governed by `constitution.md`. Extends v1; same dependency direction (lower never depends on higher).

```
Config → Tools(@Tool) → Memory(seam) → AgentService → Web(REST+SSE) → UI
                                   ↘ McpServer (exposes Tools)        Filters: RateLimit
Actuator/Micrometer = cross-cutting observability
```

## A. SSE streaming
- New `AgentService.chatStream(message, conversationId)` → returns a stream result: `Flux<String>` tokens + a completion hook delivering `toolsUsed`.
- **toolsUsed without ThreadLocal (R1 fix):** introduce a per-call `ToolCallSink` created fresh per request and passed explicitly to the `RecordingToolCallback` instances *for that call* (request-scoped tool callbacks via `.tools(...)` per-request), OR accumulate via Reactor Context. Chosen: **per-request tool callbacks bound to a fresh sink**, so capture is call-local and reactive-safe. The blocking `/api/chat` keeps working through the same sink abstraction (ThreadLocal becomes one implementation detail behind `ToolCallSink`, used only for the blocking path; streaming uses the explicit per-call sink).
- Controller: `ChatStreamController` returns `Flux<ServerSentEvent<…>>`: token events (`event: token`), then a final `event: done` with `{toolsUsed, conversationId}`, or `event: error`.
- Memory: on stream completion (doOnComplete), append user+assistant to the seam.

## B. Persistent memory (seam)
- `ChatMemoryStore` → **interface** (`history`, `append`). 
- `InMemoryChatMemoryStore` = today's logic (default bean; tests).
- `JdbcChatMemoryStore` = Postgres-backed via `JdbcTemplate`; one table `chat_message(conversation_id, seq, role, content, created_at)`; bounded read (last N per conversation) + prune on append. `@Profile("postgres")` / `@ConditionalOnProperty` on datasource.
- Schema via Flyway migration `V1__chat_message.sql`.
- Sink rule: store is a sink — `append` returns void but its effect is a row; `history` is a pure read. No hidden cross-call state beyond the DB.

## C. Rate limit + metrics
- `RateLimitFilter` (`OncePerRequestFilter`) on `/api/chat**`: Bucket4j `Bucket` per client key (`X-Forwarded-For` first hop else `getRemoteAddr()`); `ConcurrentHashMap<String,Bucket>` bounded; 429 + `Retry-After` when drained. Config: capacity + refill tokens/period.
- Metrics: `spring-boot-starter-actuator` + `micrometer-registry-prometheus`; expose only `health,info,prometheus`. Custom: `MeterRegistry` counter `agent.chat.requests`, counter `agent.tool.invocations{tool=…}` incremented in `RecordingToolCallback`.

## D. MCP server
- `spring-ai-starter-mcp-server-webmvc` (HTTP/SSE) in the web app: register the existing `ToolCallback` list (from `AgentTools`) as MCP tools via a `ToolCallbackProvider` bean. Auto-config exposes the MCP HTTP/SSE endpoint.
- stdio: a separate entry/profile `mcp-stdio` using `spring-ai-starter-mcp-server` with web turned off and banner/logging routed to stderr (clean stdout). Documented run command.

## Package layout (additions)
```
com.baha.agent
├─ agent/
│   ├─ ChatMemoryStore.java          # now INTERFACE
│   ├─ InMemoryChatMemoryStore.java  # was ChatMemoryStore impl
│   ├─ JdbcChatMemoryStore.java      # NEW @Profile(postgres)
│   ├─ ToolCallSink.java             # NEW capture abstraction (reactive-safe)
│   ├─ AgentService.java             # + chatStream(...)
│   └─ ...
├─ web/
│   ├─ ChatController.java           # unchanged
│   ├─ ChatStreamController.java     # NEW SSE
│   └─ filter/RateLimitFilter.java   # NEW
├─ config/
│   ├─ ChatClientConfig.java         # + per-request sink wiring
│   ├─ McpConfig.java                # NEW ToolCallbackProvider for MCP
│   └─ MetricsConfig.java            # NEW counters
resources/
├─ db/migration/V1__chat_message.sql # Flyway
├─ application.yml                    # + actuator, profiles
└─ static/app.js                      # streaming consumer
docker-compose.yml                    # postgres:16
```

## Security boundaries (new)
- DB creds via env (`POSTGRES_*`); never committed. JDBC store uses parameterized statements only (no string-built SQL).
- MCP HTTP endpoint exposed locally; document that it is unauthenticated (same posture as `/api/chat`) — not for public exposure.
- Actuator: only `health,info,prometheus` exposed; no `env`/`heapdump`.
- Rate-limit filter also shields the new `/stream` endpoint from token-spend abuse.
