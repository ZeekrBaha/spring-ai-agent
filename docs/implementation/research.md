# Research — spring-ai-agent

Governed by `constitution.md`.

## Goal
Build a Java AI agent: an LLM that, given a natural-language request, autonomously calls one or more registered tools (Spring beans) in a loop until it can answer. Demonstrate "Spring Beans as tools" cleanly.

## Audience
The user (Java/AI builder) + portfolio viewers. Single-user, local-first. No multi-tenant auth in MVP.

## Success criteria
- Ask "What's 17.5 * 3, and what time is it in Tokyo?" → agent calls calculator + time tools, returns one coherent answer.
- Ask "Weather in Paris right now?" → agent calls weather tool with a real API, returns current conditions.
- Adding a new tool = write one `@Tool`-annotated method on a `@Component`; no wiring changes elsewhere.
- Full suite green; live browser chat works.

## Decisions (locked with user)
- **Framework:** Spring AI 1.1 — `Constraint`. Reason: goal is literally "Spring Beans as tools"; Spring AI auto-generates a `ToolCallback` from each `@Tool` method on a bean and runs the tool-call loop via `ToolCallingAdvisor` transparently.
- **Provider:** OpenAI — `Constraint`. Via `spring-ai-starter-model-openai`. Swappable to Claude/Ollama by one property + starter swap (designed for, not built in MVP).
- **Tool domain:** Utility toolbox — calculator, current-time (timezone), weather, web-fetch.
- **Interface:** REST (`POST /api/chat`) + minimal static chat page.
- **Stack:** Java 21 (LTS), Spring Boot 4.x, Maven, JUnit 5. `Constraint`. (Overrides skill's Next.js default — this is an explicit Java request.)

## Evidence (verified 2026-06, Spring AI ref docs + release notes)
- `Evidence`: `@Tool` on bean methods → ChatClient generates `ToolCallback` per method; `ToolCallingAdvisor` auto-registered when tools present, manages the call loop.
- `Evidence`: `ChatClient.Builder.defaultTools(...)` registers tools for every request; per-request `.tools(...)` also supported.
- `Evidence`: provider swap = change `application.yml` property + swap Boot starter dependency.
- `Evidence`: Spring AI 1.1 is GA; ChatClient/MCP/Advisors APIs stable.
- `Risk/Evidence`: open issue spring-ai#5134 — `defaultTools()` may not detect `@Tool` methods during startup in some setups. Mitigation: register tools explicitly and add a startup integration test asserting tools are discoverable.

## External APIs
- `Assumption`: weather via **Open-Meteo** (free, no API key) → avoids a second secret. Geocoding via Open-Meteo geocoding endpoint.
- Web-fetch: plain HTTP GET, return extracted text (size-capped).

## Risks / unknowns
- LLM nondeterminism → tests must not assert exact LLM prose; assert tool invocation + structured fields (use a fake/mock ChatModel where possible).
- OpenAI cost → cap max tokens + conversation length; document spend.
- Tool abuse: web-fetch SSRF (internal IPs), oversized responses → validate + cap (see `design.md`).
