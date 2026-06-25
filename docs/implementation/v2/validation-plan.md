# Validation Plan — spring-ai-agent v2

Governed by `constitution.md`. Defined before coding.

## Automated gates (must stay green + gating)
| Check | Command | Pass |
|-------|---------|------|
| Unit+integration (incl. Testcontainers) | `mvn -B verify` | all green |
| Static analysis | `mvn -B spotbugs:check` | 0 bugs (documented excludes only) |
| Secret scan | `git grep -nE 'sk-[A-Za-z0-9]{10}'` + scan compose/yml for passwords | none |
| No-regression | existing v1 tests still in the suite, green | yes |

## Runtime verification (observe, don't assume) — design.md eval cases
1. SSE weather → incremental tokens, `done.toolsUsed=[weather]`, Postgres +2 rows.
2. SSE chat → tokens, `toolsUsed=[]`.
3. 21 reqs/one IP (cap 20) → ≥1 `429` + `Retry-After`.
4. Two IPs under cap → both `200`.
5. `/actuator/prometheus` contains `agent_chat_requests_total`, `agent_tool_invocations_total`.
6. MCP inspector/client lists 4 tools; calculator `2+2`→`4`.
7. Restart under `postgres` profile → previous conversation history returned.

## UI gate (streaming)
Tokens fill the bubble live; "using tools…" state before first token; chips at end; error event → error bubble (no hang); IBM Plex + teal tokens intact; AA contrast/focus preserved.

## Reactive-correctness gate
A dedicated test must demonstrate `toolsUsed` capture works when recording happens off the request thread, i.e. it would FAIL with the v1 ThreadLocal approach. This proves R1 is actually fixed, not masked.

## Refutation pass (grader ≠ doer)
Fresh agent/session: "prove acceptance criteria are NOT met." Hunt: stream that hangs on error; toolsUsed wrong/empty under streaming; SQL injection or unparameterized query in JdbcChatMemoryStore; rate-limit bypass (spoofed XFF, shared bucket across IPs, limiter off on /stream); Postgres rows not bounded; MCP exposes more/fewer than 4 tools or wrong result; actuator leaking `env`/secrets; secret in compose; app fails to boot without DB/key. Record in validation-report.md. Default "not done" until it can't break it.

## Cost note
Streaming + live evals spend OpenAI tokens (gpt-4o-mini). Document approximate spend; cap max-tokens stays.
