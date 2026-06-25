# Repo Engineering Review — spring-ai-agent (PR #1)

**Reviewed:** 2026-06-24 21:19 local · **Target:** PR #1 `feat/agent-mvp` → `main`
**Reviewer:** repo-engineering-review skill (live tree, not memory)

## About
`spring-ai-agent`: Java 21 / Spring Boot 3.5.5 + Spring AI 1.1.2 AI agent. Tool-calling
loop exposing four Spring `@Tool` beans (calculator, time, weather, web-fetch) to
gpt-4o-mini, fronted by a REST `/api/chat` endpoint and a static chat UI.

## Verdict
Code is solid and ship-ready **as an MVP** — but NOT ship-ready by the portfolio
documentation/CI standard. Strong TDD discipline and real security thought. Blockers
are process/docs, not correctness: zero CI gate, no lint/static-analysis, no dependency
vulnerability scan, thin README with no UI screenshots.

## What Was Done Well
- **Real TDD.** Commit history shows red→green per task (T2–T9, each "N tests green (TDD)").
  Tests are behavior-focused. 34 green verified live.
- **SSRF guard genuinely good** (`WebFetchTool`): scheme allowlist; blocks
  loopback / any-local / link-local / site-local / multicast / **IPv6 ULA fc00::/7**
  (the range Java's `isSiteLocalAddress()` misses); redirects pinned to NEVER so a public
  page can't 3xx-bounce internal. TOCTOU DNS-rebind residual documented honestly.
  9 block-vector tests incl. cloud-metadata `169.254.169.254`.
- **Secret hygiene clean.** Real key in gitignored `config/application.properties`,
  `.example` template tracked, `.env` ignored, placeholder `not-set` default boots keyless.
- **Testable seams via DI.** `HostResolver` + injected `Clock` keep network and wall-clock
  out of unit tests. `RecordingToolCallback` decorator captures `toolsUsed` transparently.
- **Bounded memory.** `ChatMemoryStore` caps per-conversation messages AND total
  conversations (LRU via `removeEldestEntry`), `synchronized`. Race + unbounded growth both
  closed and tested.
- Input validation (`@Valid @NotBlank @Size(4000)`), logs **tool name only** (no
  sensitive-arg leak), calculator overflow guard at 2^53.

## What Was Done Badly
- **No CI** — `.github/workflows` absent. PR claims "34 tests" but nothing enforces it on
  push/merge. **(High)**
- **No lint / static analysis / format gate.** No Spotless, Checkstyle, SpotBugs, PMD.
  Type checking is bare `javac`. **(Medium)**
- **No dependency vulnerability scan.** No OWASP dependency-check / `mvn versions`. Cannot
  claim deps clean. **(Medium)**
- **`GlobalExceptionHandler` catches ALL `RuntimeException` → 502.** A genuine app bug
  (NPE, ISE) is masked as "upstream error," hiding defects. **(Medium)**
- **README thin** (46 lines, 8 one-line sections). Missing repo map, architecture depth,
  limitations/next-steps. **UI repo with zero screenshots** — skill standard requires a
  `## Screenshots` section. **(Medium — blocks ship-ready label)**
- **`/api/chat` open + unauthenticated** — spends OpenAI tokens, no rate limit. Fine local,
  abuse/cost risk if exposed. **(Low)**

## README
Exists, 8 sections (What it does / Stack / Run / API / Test / Security notes / Configuration),
all shallow one-liners. Missing: repo map (§11), limitations/next-steps (§14),
architecture/mental-model depth, and screenshots of the chat UI. Not comprehensive by
portfolio standard.

## TDD / Tests
Real TDD evidence from commit cadence and red-test notes. `mvn test` →
`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`. No-LLM-in-tests
policy enforced (model mocked). Honest gap: the SSRF runtime block path is never exercised
through the LLM (gpt-4o-mini self-declines localhost), so it is covered by 9 `WebFetchTool`
unit tests instead.

## Lint / Type / CI
None configured. Minimal add: GitHub Action running `mvn -B verify`, plus Spotless
(google-java-format) and OWASP dependency-check wired into the build.

## Security / Vulnerabilities
- **Confirmed good:** SSRF defenses, secret hygiene, tool-name-only logging.
- **Likely risk:** broad `RuntimeException`→502 masks real bugs; open token-spending endpoint.
- **Unknown (needs tooling):** transitive CVEs — no `dependency-check`/audit ran.

## How To Improve (ordered by impact)
1. Add `.github/workflows/ci.yml` running `mvn -B verify` on push/PR. Closes the
   enforcement gap.
2. Narrow `GlobalExceptionHandler` — return 502 for model/upstream exceptions only; let
   genuine bugs surface as 500 (or log + distinguish).
3. Add Spotless + OWASP dependency-check to `pom.xml`; wire into CI.
4. Expand README: repo map, architecture, limitations, `## Screenshots` of the chat UI.

## How To Enhance
Streaming responses (SSE), per-IP rate limiting on `/api/chat`, tool-call tracing/metrics
(Micrometer/OpenTelemetry), persistent conversation memory behind the existing store
seam, per-tool timeouts.

## Verification
- `git status` — on `feat/agent-mvp`, clean. PR #1 OPEN, base `main`.
- `mvn test` → `Tests run: 34, Failures: 0, Errors: 0` / `BUILD SUCCESS`.
- `ls .github/workflows` → `NO CI WORKFLOWS`.
- Source read: all 16 main + 10 test files, `pom.xml`, `application.yml`, `.gitignore`,
  `config/application.properties.example`, README.
- No repo edits made beyond this review artifact.

## Fixes Applied (2026-06-25)
- **CI added** — `.github/workflows/ci.yml`: `mvn -B verify` + SpotBugs on push/PR, GitHub dependency-review (fail-on high CVE) on PRs. Closes the #1 enforcement gap. *(High — resolved)*
- **GlobalExceptionHandler narrowed** (TDD) — only `AgentUpstreamException` → 502; other `RuntimeException` → 500 (logged); validation stays 400. Genuine bugs no longer masked. New tests: `genuineBugSurfacesAs500NotMaskedAs502`, `modelCallFailureIsWrappedAsUpstreamException`. *(Medium — resolved)*
- **README expanded** — repo map, architecture, limitations/next-steps, and a `## Screenshots` section with a real captured chat-UI image (`docs/screenshots/chat.png`). *(Medium — resolved)*
- **Dependency vuln scan** — GitHub dependency-review action on PRs (reliable, no NVD key); SpotBugs static analysis wired into CI. *(Medium — resolved)*
- **Deliberately deferred:** Spotless/google-java-format (mass-reformat churn outweighs value now); per-IP rate limiting on `/api/chat` (tracked under README "Limitations / next steps", low risk for local use).

Test count: 34 → **36**. `mvn verify` green.
