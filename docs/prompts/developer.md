# Developer Prompt — spring-ai-agent

Read first: `docs/implementation/constitution.md`. It governs you. Hard rules (verbatim):

1. **No production code without a failing test first.** RED → run it → watch it fail for the right reason → GREEN minimal code → run → pass → refactor. Code written before its test: delete and restart.
2. **Security from line one.** `OPENAI_API_KEY` from env only — never in source/yml/git. No secret in browser/JS. Validate tool inputs at the boundary.
3. **Anti-slop.** UI: paste the `design-system.md` token block verbatim. Banned: Inter/Roboto/Arial, gradients/gradient text, emoji-as-icons, hero→3-cards template, lorem ipsum. Real data only.
4. **No invented business rules.** Mark unknowns `ASSUMPTION`, escalate.
5. **Verify in runtime**, not compile-clean.
6. **Simplest code that passes acceptance criteria.** No speculative abstraction, no defensive bloat, no drive-by refactors.

## Your scope
Implement tasks T0–T8 from `implementation-plan.md` in order. One task at a time. After EACH task: run the relevant test, then update `progress.md` (done+verified / in-flight / blocked / decisions).

## Per-task loop
1. Read the task row in `implementation-plan.md` + linked requirement in `requirements.md`.
2. Write the RED test named in the table. Run it. Confirm it fails for the right reason.
3. Write minimal code to pass. Run. Confirm green. Confirm prior tests still green.
4. Refactor if needed, keep green.
5. Update `progress.md`.

## Stack specifics
- Java 21, Spring Boot 4.x, Maven, Spring AI 1.1, JUnit 5.
- **Before pinning deps (T1): verify exact coordinates via context7 (`/spring-projects/spring-ai`) or start.spring.io — do NOT guess versions.** Starter: `spring-ai-starter-model-openai`.
- Tools = `@Component` beans with `@Tool`-annotated methods. Register via `ChatClientConfig`. Let `ToolCallingAdvisor` run the loop — do NOT hand-roll the tool-call loop.
- Calculator: use a safe arithmetic evaluator, never `eval`/script engine on raw input.
- Weather: Open-Meteo (no key). Mock the HTTP client in tests.
- WebFetch: scheme allowlist (http/https), reject private/loopback/link-local hosts (SSRF), cap size + timeout. Write the SSRF block test FIRST.
- LLM tests: mock/fake `ChatModel`; assert tool invoked + structured fields, never exact prose.

## UI (T8)
Vanilla HTML/CSS/JS, no framework. Paste tokens from `design-system.md`. Build all 4 states (empty/loading/error/success). Show `toolsUsed` chips. Enter sends, Shift+Enter newline. AA contrast + focus rings + aria-labels.

## Report after each task
State: task id, files changed, test names + RED→GREEN result, command output summary. Do not claim done on build success alone.

## Stop conditions — escalate, don't guess
- Dependency coordinate uncertain → verify or ask.
- A business rule not in the docs → mark ASSUMPTION + ask.
- Cannot test a behavior → flag it; do not write untestable code.
