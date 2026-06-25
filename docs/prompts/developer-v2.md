# Developer Prompt — spring-ai-agent v2

Read first: `docs/implementation/v2/constitution.md` (+ v1 `docs/implementation/constitution.md`). They govern you. Hard rules (verbatim):

1. **No production code without a failing test first.** RED → run → watch it fail for the right reason → GREEN minimal → run → pass → refactor. Code before its test: delete, restart.
2. **No regressions.** v1 is shipped (37 tests). Keep the FULL existing suite green after every task. Don't break `/api/chat`, the 4 tools, the SSRF guard, or memory.
3. **Security from line one.** DB creds + key via env / gitignored `config/` only. JDBC = parameterized statements only. Actuator exposes only `health,info,prometheus`. No secret in source/compose/jar.
4. **Reactive correctness.** ThreadLocal is INVALID across a Flux. The streaming path captures `toolsUsed` via `ToolCallSink` (per-call), proven by a test that would fail under ThreadLocal.
5. **Minimal, behind seams.** Persistent memory goes behind the existing `ChatMemoryStore` seam (interface + InMemory default + Jdbc). No rewrite of working v1 code beyond need.
6. **Verify in runtime, not compile-clean.** Done = suite green AND observed working (SSE streams in browser; 429 past limit; rows in Postgres; metric scraped; MCP tool callable).
7. **Gates stay gating.** `mvn verify` + `spotbugs:check` + dependency-review green. New deps pass the CVE gate; SpotBugs clean (exclude only documented noise).

## Scope
Tasks B-T1 → D-T2 then V, in `implementation-plan.md` order (B→A→C→D). One task at a time. After EACH: run relevant tests + full suite, update `docs/implementation/v2/progress.md`.

## Per-task loop
1. Read the task row + linked requirement. 2. Write the named RED test, run, confirm right-reason failure. 3. Minimal GREEN. 4. Run test + full suite green. 5. Refactor green. 6. Update progress.md. 7. Commit (per-task, message states task id + RED→GREEN + test counts).

## Stack specifics
- Java 21, Spring Boot 3.5.5, Spring AI 1.1.2, Maven, JUnit 5.
- **Verify each new dep's coordinates via context7 (`/spring-projects/spring-ai`) or start.spring.io before pinning. Do NOT guess versions.**
- Streaming: `chatClient.prompt()...stream().content()` → `Flux<String>`. If tool-calling breaks streaming, fall back to `.chatClientResponse()` + `ChatClientMessageAggregator`.
- Memory: extract interface first (B-T1, pure refactor). Jdbc store via `JdbcTemplate`, Flyway migration, Testcontainers for IT. Run one contract test against BOTH impls.
- Rate limit: Bucket4j `Bucket` per IP in a `OncePerRequestFilter`; 429 + `Retry-After`; key by first `X-Forwarded-For` hop else `getRemoteAddr()`.
- Metrics: actuator + micrometer-registry-prometheus; counters `agent.chat.requests`, `agent.tool.invocations{tool}`.
- MCP: `spring-ai-starter-mcp-server-webmvc` (HTTP/SSE) registering existing `ToolCallback`s via a `ToolCallbackProvider`; stdio under `mcp-stdio` profile with clean stdout (logs→stderr, web off).

## Report after each task
task id · files changed · test names + RED→GREEN result · command summary. Never claim done on build success alone.

## Stop & escalate (don't guess)
Dep coordinate uncertain · a business rule not in docs · can't test a behavior · stdio MCP stdout pollution unresolved → flag, don't hack around it.
