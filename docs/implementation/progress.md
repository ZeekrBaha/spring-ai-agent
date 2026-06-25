# Progress — spring-ai-agent (live handoff log)

Re-read this first on any fresh/resumed session. Update after EVERY task.

## Status: SPEC COMPLETE — awaiting user approval to build. No code written yet.

## Done + verified
- Spec package authored: constitution, research, requirements, architecture, design, design-system, implementation-plan, validation-plan, prompts.
- Key decisions locked: Spring AI 1.1 + Spring Boot 4 + Java 21 + Maven; OpenAI provider (env key); utility toolbox (calc/time/weather/web-fetch); REST + minimal chat UI.

## In flight
- None. Gate: user approval.

## Blocked / unverified
- Spring Boot 4.x + Spring AI 1.1 exact dependency coordinates — VERIFY via context7/start.spring.io at T1 before pinning.
- `toolsUsed` capture mechanism — spike in T6.
- spring-ai#5134 startup tool detection — covered by T6b test.

## Decisions / rejected
- Rejected LangChain4j: goal is "Spring Beans as tools" → Spring AI is the direct fit.
- Rejected Ollama/Claude for MVP: user chose OpenAI. Provider-swap seam kept in architecture.
- Weather via Open-Meteo (no API key) to avoid a second secret.

## Next task when approved
T0 scaffold → T1 deps (verify coordinates first).
