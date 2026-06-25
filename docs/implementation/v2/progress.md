# Progress — spring-ai-agent v2 (live handoff log)

Re-read first on any fresh/resumed session. Update after EVERY task.

## Status: PLAN COMPLETE — awaiting approval. No v2 code written. v1 is merged on main (37 tests, gating CI).

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
