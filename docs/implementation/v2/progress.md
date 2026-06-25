# Progress — spring-ai-agent v2 (live handoff log)

Re-read first on any fresh/resumed session. Update after EVERY task.

## Status: BUILDING on feat/v2-extensions. Feature B (Postgres memory) DONE. Now Feature A (streaming).

### Env note (Docker 29 + Testcontainers)
Docker Engine 29 min API = 1.44; Testcontainers' docker-java 3.4.2 probes v1.43 → 400. Fixed locally via `~/.docker-java.properties` (`api.version=1.44` + DOCKER_HOST). CI's Docker is compatible. ITs run under maven-failsafe in the verify phase.

### Done + verified
- B-T1: `ChatMemoryStore` interface + `InMemoryChatMemoryStore` (refactor, 37 green).
- B-T2: `JdbcChatMemoryStore` + Flyway `V1__chat_message.sql` + Testcontainers IT (5). Failsafe wired.
- B-T3: postgres profile (re-enables JDBC/Flyway), docker-compose.yml; MemoryWiring tests. App boots DB-less AND under postgres profile. SpotBugs clean.

## Original plan status below.

## Done + verified
- v2 plan authored under `docs/implementation/v2/`: constitution, research, requirements, architecture, design, implementation-plan, validation-plan + prompts.
- Decisions locked: MCP both stdio+HTTP/SSE; Postgres via Docker Compose + Testcontainers; Bucket4j in-memory per-IP; one branch `feat/v2-extensions`, 4 phases, one PR.
- APIs verified (Spring AI 1.1): `stream().content()`→Flux; `spring-ai-starter-mcp-server(-webmvc)`; `...-chat-memory-repository-jdbc`.

## In flight
- None. Gate: user approval.

## Blocked / unverified (resolve at build)
- Exact dep coordinates — verify via context7 before pinning each.
- R1 ThreadLocal-vs-stream — fixed by `ToolCallSink` (A-T1); must be proven by a test that fails under ThreadLocal.
- R3 stdio MCP clean-stdout — riskiest; isolated in `mcp-stdio` profile (D-T2), doesn't block HTTP MCP.

## Decisions / rejected
- Memory: keep our `ChatMemoryStore` seam (interface + InMemory + Jdbc) rather than replacing wholesale with Spring AI `JdbcChatMemoryRepository` — honors "behind the existing seam" + keeps bounded/LRU semantics + existing tests.
- Build order B→A→C→D (de-risk: seam first, hardest bug while fresh, MCP last).

## Next task when approved
B-T1: extract `ChatMemoryStore` interface + `InMemoryChatMemoryStore`, retarget existing memory tests, keep green.
