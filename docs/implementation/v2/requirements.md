# Requirements — spring-ai-agent v2

Governed by `constitution.md`. Each requirement has acceptance criteria + maps to a task.

## Feature A — SSE streaming
| ID | Requirement | Acceptance criteria | Task |
|----|-------------|---------------------|------|
| A1 | Streaming endpoint | `GET/POST /api/chat/stream` returns `text/event-stream`; emits token chunks as SSE `data:` events; terminal event carries final `toolsUsed` + `conversationId`. | A-T2 |
| A2 | Streaming tool capture | `toolsUsed` is correct under streaming (NOT via ThreadLocal). Proven by a test that fails with the ThreadLocal approach. | A-T1 |
| A3 | Memory on stream | Streamed turn persists user+assistant messages to the memory seam, same as `/api/chat`. | A-T2 |
| A4 | UI streams tokens | Chat UI consumes the stream; assistant bubble fills token-by-token; shows a "using tools…" state until first token; renders tool chips at end. | A-T3 |
| A5 | Stream error | Upstream failure mid-stream emits a terminal `error` event; UI shows the error bubble, not a hang. | A-T2 |

## Feature B — Persistent memory (Postgres)
| ID | Requirement | Acceptance criteria | Task |
|----|-------------|---------------------|------|
| B1 | Memory seam is an interface | `ChatMemoryStore` becomes an interface; existing in-memory logic → `InMemoryChatMemoryStore` (default for tests). All v1 memory tests still pass against it. | B-T1 |
| B2 | JDBC-backed store | `JdbcChatMemoryStore` persists/loads history in Postgres; keeps bounded-per-conversation semantics. Verified with Testcontainers (real Postgres). | B-T2 |
| B3 | Schema | Flyway (or `schema.sql`) creates the messages table; applied on startup. | B-T2 |
| B4 | Profile select | Postgres store active under a `postgres` profile / when datasource configured; in-memory otherwise. App still boots with no DB. | B-T3 |
| B5 | Compose | `docker-compose.yml` runs `postgres:16`; documented env vars; no secret committed. | B-T3 |

## Feature C — Rate limit + metrics
| ID | Requirement | Acceptance criteria | Task |
|----|-------------|---------------------|------|
| C1 | Per-IP rate limit | Bucket4j filter on `/api/chat` + `/api/chat/stream`; over the limit → HTTP 429 + `Retry-After`; under → passes. Configurable capacity/refill. Unit-tested. | C-T1 |
| C2 | Limit isolation | Two different IPs have independent buckets. Tested. | C-T1 |
| C3 | Metrics endpoint | Actuator + Prometheus registry; `/actuator/prometheus` exposes JVM/HTTP metrics. | C-T2 |
| C4 | Custom metrics | Counter for chat requests + per-tool invocation count (tag by tool name) exported. Tested via `MeterRegistry`. | C-T2 |

## Feature D — MCP server
| ID | Requirement | Acceptance criteria | Task |
|----|-------------|---------------------|------|
| D1 | Tools over MCP | The 4 `ToolCallback` beans are exposed as MCP tools (auto-registered, de-duped by name). | D-T1 |
| D2 | HTTP/SSE transport | MCP reachable over HTTP/SSE from the running web app; tools list + call verified via MCP inspector or a test client. | D-T1 |
| D3 | stdio transport | A `mcp-stdio` profile runs an MCP server over stdio with a clean stdout (no banner/log pollution). Documented launch command for Claude Desktop/Cursor. | D-T2 |
| D4 | Parity | MCP tool call produces the same result as the in-app tool (e.g. calculator `2+2`→`4`). Tested. | D-T1 |

## Non-functional
- N1 Full existing suite + new tests green; `mvn verify` + `spotbugs:check` gating.
- N2 No secret in source/compose/jar. New boundaries validated.
- N3 App boots with NO database and NO key (degrades, as v1).
- N4 SpotBugs clean (exclude only documented noise).

## Non-goals (v2)
- Auth / multi-user accounts. Redis-distributed rate limit. Horizontal scaling of memory. Streaming over MCP tool results. RAG / vector store. Persisting `toolsUsed` history table. UI redesign beyond the streaming bubble.
