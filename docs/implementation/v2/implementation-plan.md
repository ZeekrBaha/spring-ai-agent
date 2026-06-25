# Implementation Plan — spring-ai-agent v2

Governed by `constitution.md`. Branch `feat/v2-extensions` off `main`. Every task TDD (RED→GREEN watched). Keep full existing suite green after each. Update `progress.md` after every task. Verify dep coordinates (context7) before pinning.

Order chosen to de-risk: **B (memory seam) → A (streaming) → C (rate/metrics) → D (MCP)**. Reason: making the memory seam an interface first is low-risk groundwork; streaming carries the hardest bug (R1) so do it while context is fresh; rate/metrics is independent; MCP last (most external-config risk, R3).

| Task | Goal | Test-first (RED) | Acceptance | Risk/rollback |
|------|------|------------------|------------|---------------|
| **B-T1** | Extract `ChatMemoryStore` interface; `InMemoryChatMemoryStore` impl | existing memory tests retarget the interface + impl; all still green | B1 | pure refactor; revert if red |
| **B-T2** | `JdbcChatMemoryStore` + Flyway schema, Testcontainers | `JdbcChatMemoryStoreIT` (Testcontainers PG): append→history, bounded prune, restart-survives | B2,B3 | container needed; skip-able by tag locally, runs in CI |
| **B-T3** | Profile wiring + compose | test: in-memory active by default (no DB); jdbc bean under `postgres` profile | B4,B5 | app must still boot keyless+DB-less |
| **A-T1** | `ToolCallSink` reactive-safe capture; refactor recorder | `ToolCallSinkTest`: capture correct when names recorded from another thread (fails under ThreadLocal) | A2 | core of R1 fix |
| **A-T2** | `AgentService.chatStream` + `ChatStreamController` (SSE) | `ChatStreamControllerTest` (WebMvc/WebTestClient, mocked service): token events + terminal done; error event on failure; memory append | A1,A3,A5 | if stream+tools misbehaves, use chatClientResponse()+aggregator |
| **A-T3** | UI streaming consumer | manual + Anti-Slop gate: tokens fill bubble, "using tools" state, chips at end | A4 | vanilla JS EventSource/fetch-stream |
| **C-T1** | `RateLimitFilter` (Bucket4j) | `RateLimitFilterTest`: N pass then 429+Retry-After; two IPs independent | C1,C2 | tune defaults via config |
| **C-T2** | Actuator + Micrometer counters | `MetricsTest`: `MeterRegistry` has `agent.chat.requests` after a call; `agent.tool.invocations{tool}` increments | C3,C4 | expose only health,info,prometheus |
| **D-T1** | MCP HTTP/SSE exposes 4 tools | `McpToolsIT`: tools list = 4 names; call calculator→4 (parity) | D1,D2,D4 | webmvc MCP starter |
| **D-T2** | stdio MCP profile + docs | smoke: `mcp-stdio` profile starts, stdout clean (no banner) ; README launch cmd | D3 | isolate; HTTP MCP already covers D1/D2/D4 |
| **V** | Full validation + refutation | whole suite + 7 eval cases + fresh-agent refutation | validation-report.md | — |

## Dependencies to add (verify coordinates at first use)
spring-ai-starter-model-chat-memory-repository-jdbc · spring-ai-starter-mcp-server(+ webmvc) · spring-boot-starter-jdbc · org.postgresql:postgresql · org.flywaydb:flyway-core(+ flyway-database-postgresql) · spring-boot-starter-actuator · io.micrometer:micrometer-registry-prometheus · com.bucket4j:bucket4j_jdk17-core (or bucket4j-core) · org.testcontainers:postgresql + junit-jupiter (test) · spring-boot-starter-webflux or reactor-test as needed for SSE/Flux test.

## Sequencing notes
B-T1 unblocks everything memory. A-T1 must precede A-T2 (sink before stream). C and D independent of A/B but share the rate-limit filter coverage of `/stream` (do C after A so the filter covers both endpoints). V last.

## Token block
A-T3 (UI) reuses the EXISTING `design-system.md` tokens verbatim (IBM Plex + teal). No new aesthetic.
