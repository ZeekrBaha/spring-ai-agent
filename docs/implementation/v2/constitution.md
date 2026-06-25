# Constitution — spring-ai-agent v2 (extensions)

Extends v1 `../constitution.md` (still in force). This file restates the hard rules and adds v2-specific ones. If anything conflicts, **this + v1 constitution win over any other doc**.

Inherited from `~/.claude/CLAUDE.md` (Karpathy + mandatory TDD).

## Hard rules (verbatim into every prompt)
1. **Test-first (Iron Law).** No production code without a failing test first. RED → watch it fail for the right reason → GREEN minimal → watch it pass → REFACTOR. Code before its test is deleted and restarted.
2. **Security from line one.** Secrets (`OPENAI_API_KEY`, DB password) via env / gitignored `config/` only — never in source, never in the jar, never to the browser. New surface (DB, MCP, SSE) validated at boundaries.
3. **No regressions.** v1 is shipped and green (37 tests). Every v2 task keeps the **full existing suite green**. Don't break `/api/chat`, the 4 tools, the SSRF guard, or the existing memory seam.
4. **Minimal, behind seams.** Persistent memory swaps in behind the *existing* `ChatMemoryStore` seam (make it an interface; in-memory stays the default for tests). No rewrite of working v1 code beyond what each feature requires.
5. **No invented rules.** Unknown behavior → mark `ASSUMPTION` / escalate.
6. **Verify in runtime, not compile-clean.** Done = suite green AND the feature observed working live (SSE streams in a browser; MCP tool callable from a client/inspector; rows land in Postgres; 429 returned past the limit; metric scraped).
7. **Grader ≠ doer.** Before "done", a fresh refutation pass tries to prove acceptance criteria are NOT met.
8. **Gates stay gating.** `mvn verify` + SpotBugs + dependency-review must stay green and blocking. New deps pass the CVE gate.

## v2-specific
- **Reactive correctness.** Streaming hops threads — `ThreadLocal` state (the v1 `toolsUsed` recorder) is INVALID across a `Flux`. Any per-request capture in the streaming path must use a stream-scoped mechanism (Reactor Context or a per-call accumulator), proven by a test that would fail under the ThreadLocal approach.
- **Containers in tests.** DB integration tests use Testcontainers (real Postgres), not mocks, for the persistence layer. Unit tests elsewhere stay mock-based per v1 policy.
- **No secret in compose.** `docker-compose.yml` uses env-var interpolation with safe local-only defaults; document required vars.
