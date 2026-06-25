# spring-ai-agent

A small Java AI agent: an LLM that calls **Spring beans as tools** in a loop until it can answer. Built with Spring Boot 3.5 + Spring AI 1.1.

## What it does
Ask in natural language; the agent routes to the right tool(s) and composes one answer:
- **calculator** — arithmetic
- **time** — current time in an IANA timezone
- **weather** — current conditions for a city (Open-Meteo, no API key)
- **web-fetch** — readable text from a public page (SSRF-guarded)

Each tool is a `@Component` with a `@Tool`-annotated method. Adding a tool = one annotated method; `ChatClient` + `ToolCallingAdvisor` run the loop. Responses report which tools fired (`toolsUsed`).

## Stack
Java 21 · Spring Boot 3.5.5 · Spring AI 1.1.2 · Maven · REST + a vanilla chat UI.

## Run
```bash
export OPENAI_API_KEY=sk-...        # required for live chat
mvn spring-boot:run
# open http://localhost:8080
```
The app boots and serves the UI even without a key; chat calls then return a friendly 502.

## API
`POST /api/chat`
```json
{ "message": "weather in Paris and what's 12*9?", "conversationId": "optional" }
→ { "reply": "...", "toolsUsed": ["weather","calculator"], "conversationId": "..." }
```

## Test
```bash
mvn verify     # 34 tests, build, package
```
All code was written test-first (TDD). See `docs/implementation/` for the spec, plan, and `validation-report.md` (incl. an adversarial refutation pass).

## Security notes
- API key via env var only; never in source. No secret reaches the browser.
- web-fetch blocks loopback/private/link-local/IPv6-ULA targets and disables redirects. Known residual: DNS-rebinding TOCTOU (see `validation-report.md`).

## Configuration
| Property | Default | Meaning |
|----------|---------|---------|
| `OPENAI_API_KEY` | `not-set` | OpenAI key (env var) |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | model |
