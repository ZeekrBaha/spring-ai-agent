# Progress — spring-ai-agent (live handoff log)

Re-read this first on any fresh/resumed session.

## Status: COMPLETE — 34 tests green, refutation fixed, LIVE LLM EVAL PASSED (2026-06-25, gpt-4o-mini). All gates closed.

## Done + verified (TDD, RED→GREEN watched each task)
- T0/T1 scaffold — **Spring Boot 3.5.5 + Spring AI 1.1.2 + Java 21** (NOT Boot 4; see decision). contextLoads green.
- T2 CalculatorTool (exp4j) — 7 tests. T3 TimeTool (injected Clock) — 3. T4 WeatherTool (Open-Meteo, mocked boundary) — 2. T5 WebFetchTool + SSRF — 9.
- T6 AgentService + tool-loop + `toolsUsed` ThreadLocal capture; T6b startup ToolRegistrationTest (mitigates spring-ai#5134) — 4.
- T7 REST `POST /api/chat` + bounded memory + validation + 502 handler — controller 3, memory 4, service 2.
- T8 vanilla chat UI — IBM Plex + teal tokens, 4 states, toolsUsed chips, a11y, XSS-safe (textContent).
- T9 validation: `mvn verify` green; no secrets; app boots without key (placeholder), serves UI, blank→400, valid→friendly 502. Refutation pass run → 7 findings fixed (see validation-report.md). Suite 34 green.

## In flight
- None. Build complete on branch `feat/agent-mvp` (not merged).

## Blocked / needs user
- **Live LLM tool-routing eval (eval cases 1–6) requires a real OPENAI_API_KEY** — run `OPENAI_API_KEY=sk-... mvn spring-boot:run`, open http://localhost:8080.

## Decisions / rejected
- **Boot 3.5.5 not Boot 4**: Spring AI 1.1 GA targets Boot 3.4/3.5; Boot 4 needs Spring AI 2.0 (milestone). Chose stable paved path. (Spec said Boot 4 — corrected after verifying coordinates.)
- `api-key` default `not-set`: app boots + serves UI without a key (friendly 502 on chat) instead of a startup crash.
- DNS-rebinding TOCTOU in web-fetch: documented residual; redirects pinned NEVER, ULA+literal vectors closed. Full socket-pin deferred.
- exp4j calculator allows `^`/`sqrt` etc. — broader than description; no security impact; accepted.
- Rejected LangChain4j (Spring AI is the direct fit for "Spring Beans as tools"); rejected Ollama/Claude for MVP (user chose OpenAI).

## Next when resumed
Either: run the live LLM eval, or finish the branch (merge / PR) via finishing-a-development-branch.
