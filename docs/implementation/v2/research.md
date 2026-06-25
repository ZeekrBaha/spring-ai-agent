# Research — spring-ai-agent v2 extensions

Governed by `constitution.md`. Target repo: `/Users/baha/Desktop/llm-ai-projects/spring-ai-agent` on `main` (v1 merged, 37 tests, CI gating).

## Goal
Add 4 README "next-steps" features to the live agent: SSE streaming, MCP server, persistent (Postgres) memory, per-IP rate limit + Micrometer metrics.

## Repository facts (v1, verified)
- `Repository fact`: Java 21, Spring Boot 3.5.5, Spring AI 1.1.2, Maven, JUnit 5. 37 tests, `mvn verify` + `spotbugs:check` green.
- `Repository fact`: `AgentService.chat(message, conversationId)` → blocking `ChatClient.prompt()...call()`, captures `toolsUsed` via `ToolCallRecorder` (ThreadLocal) + `RecordingToolCallback`.
- `Repository fact`: `ChatMemoryStore` is a concrete `@Component` (in-memory, bounded per-conv + LRU total). This is the seam to extend.
- `Repository fact`: tools = 4 `@Component` `@Tool` beans, registered as `ToolCallback[]` in `ChatClientConfig`, held in `AgentTools(List<ToolCallback>)`.
- `Repository fact`: REST `POST /api/chat`; `GlobalExceptionHandler` maps `AgentUpstreamException`→502, other→500, validation→400.
- `Repository fact`: static chat UI (`app.js`) does one `fetch('/api/chat')` per turn, renders reply + tool chips.

## Decisions (locked with user)
- **MCP transport:** BOTH stdio + HTTP/SSE — `Constraint`.
- **Postgres:** Docker Compose (`postgres:16`) + Testcontainers in tests — `Constraint`.
- **Rate limit:** Bucket4j in-memory per-IP token bucket — `Constraint`.
- **Delivery:** one branch `feat/v2-extensions`, 4 TDD phases, one PR — `Constraint`.

## Evidence (Spring AI 1.1 ref docs, verified 2026-06)
- `Evidence`: `ChatClient.prompt()...stream().content()` returns `Flux<String>` (token chunks). Also `.chatResponse()` / `.chatClientResponse()` for metadata.
- `Evidence`: streaming + tool calls — the model may emit tool calls before final text; Spring AI's advisor handles the loop, but tool-call chunks interleave. `ChatClientMessageAggregator` exists for manual aggregation.
- `Evidence`: MCP server via `spring-ai-starter-mcp-server` (and `...-mcp-server-webmvc` for HTTP); auto-registers `ToolCallback` beans, de-dupes by name. Stateless streamable-HTTP variant documented.
- `Evidence`: persistent memory via `spring-ai-starter-model-chat-memory-repository-jdbc` → `JdbcChatMemoryRepository`; ships schema for common DBs.
- `Evidence`: Spring Boot Actuator + Micrometer expose `/actuator/prometheus`; `@Timed` / `MeterRegistry` for custom metrics.
- `Evidence`: Bucket4j has a Spring Boot starter; or use core `Bucket` in a `OncePerRequestFilter` keyed by client IP.

## Risks / unknowns
- **R1 (high): `toolsUsed` under streaming.** ThreadLocal recorder is wrong across `Flux` threads. Must redesign capture for the streaming path. Decision in design.md.
- **R2: streaming tool UX.** While tools run, no text streams — UI needs a "thinking/using tools" state before tokens arrive. `toolsUsed` only known at stream end.
- **R3: MCP "both" transports.** stdio usually wants a dedicated launch/profile (no web server banner on stdout — it corrupts the protocol). Likely a `mcp-stdio` Spring profile separate from the web app. `ASSUMPTION` until validated in build.
- **R4: Postgres dialect vs existing seam.** Choice: (a) replace our store with Spring AI `JdbcChatMemoryRepository`, or (b) keep our `ChatMemoryStore` interface, add a JDBC-backed impl. Design picks (b) to honor "behind the existing seam" + keep our bounded/LRU semantics and tests.
- **R5: rate-limit keying.** Client IP behind a proxy needs `X-Forwarded-For` handling; for local/single-node use `request.getRemoteAddr()`. Document the limitation.
- **R6: Testcontainers in CI.** GitHub Actions ubuntu runner has Docker — Testcontainers works. Adds runtime; acceptable.

## External services / deps (to pin at build, verify coordinates first)
`spring-ai-starter-model-chat-memory-repository-jdbc`, `spring-ai-starter-mcp-server` (+ webmvc variant), `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, Bucket4j, `spring-boot-starter-jdbc`/`postgresql` driver, Testcontainers (`postgresql`, `junit-jupiter`).
