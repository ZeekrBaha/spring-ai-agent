# spring-ai-agent

[![CI](https://github.com/ZeekrBaha/spring-ai-agent/actions/workflows/ci.yml/badge.svg)](https://github.com/ZeekrBaha/spring-ai-agent/actions/workflows/ci.yml)

A small Java AI agent: an LLM that calls **Spring beans as tools** in a loop until it can answer. Spring Boot 3.5 + Spring AI 1.1, fronted by a REST endpoint and a vanilla chat UI.

## Screenshots
A single turn routing to two tools — the reply shows which tools fired (`calculate`, `currentTime`):

![Chat UI](docs/screenshots/chat.png)

## What it does
Ask in natural language; the agent routes to the right tool(s) and composes one answer:
- **calculator** — arithmetic
- **time** — current time in an IANA timezone
- **weather** — current conditions for a city (Open-Meteo, no API key)
- **web-fetch** — readable text from a public page (SSRF-guarded)

Each tool is a `@Component` with a `@Tool`-annotated method. Adding a tool = one annotated method; `ChatClient` + `ToolCallingAdvisor` run the loop. Each response reports which tools fired (`toolsUsed`).

## Stack
Java 21 · Spring Boot 3.5.5 · Spring AI 1.1.2 · Maven · REST + static chat UI.

> Note: Spring AI 1.1 is GA against Spring Boot 3.4/3.5 — **not** Boot 4 (which needs Spring AI 2.0). This project pins the stable 3.5 line deliberately.

## Run
```bash
# Option A — env var
export OPENAI_API_KEY=sk-...
mvn spring-boot:run            # open http://localhost:8080

# Option B — local file (gitignored, never packaged)
cp config/application.properties.example config/application.properties
# paste your key into config/application.properties, then:
mvn spring-boot:run
```
The app boots and serves the UI even without a key; chat calls then return a friendly `502`.

## API
`POST /api/chat`
```json
{ "message": "weather in Paris and what's 12*9?", "conversationId": "optional" }
→ { "reply": "...", "toolsUsed": ["weather","calculator"], "conversationId": "..." }
```
Status codes: `200` ok · `400` blank/too-long message · `502` upstream model failure · `500` internal bug (distinct, not masked).

## Architecture
Fixed dependency direction (lower never depends on higher):
```
Config (app.yml, ChatClientConfig) → Tools (@Tool beans) → AgentService (ChatClient) → Controller (REST) → UI (static)
```
- **Tool loop** is owned by Spring AI's `ToolCallingAdvisor` — we don't hand-roll it.
- **`toolsUsed`** is captured by wrapping each `ToolCallback` in a `RecordingToolCallback`; a `ThreadLocal` recorder scopes captured names to one synchronous request.
- **Tools are sinks**: calculator/time are pure; weather/web-fetch each have one declared outbound-HTTP boundary, visible in the return value.
- **Memory**: `ChatMemoryStore` is in-memory, bounded per conversation (20 messages) and in total (1000 conversations, LRU).
- **Provider seam**: swapping OpenAI→Ollama/Claude is a starter dependency + property change; `AgentService` depends only on `ChatClient`.

## Repo map
```
src/main/java/com/baha/agent/
├─ Application.java
├─ config/      ChatClientConfig (registers tools), AgentTools
├─ tools/       CalculatorTool, TimeTool, WeatherTool, WebFetchTool   (@Tool beans)
├─ agent/       AgentService, ChatMemoryStore, ToolCallRecorder,
│               RecordingToolCallback, ChatResult, AgentUpstreamException
└─ web/         ChatController, GlobalExceptionHandler, dto/{ChatRequest,ChatResponse}
src/main/resources/
├─ application.yml
└─ static/      index.html, app.js, styles.css   (vanilla chat UI)
docs/
├─ implementation/   spec → plan → validation-report (incl. refutation pass)
├─ reviews/          repo engineering review
└─ screenshots/
.github/workflows/ci.yml
```

## Test & quality
```bash
mvn verify                                          # 36 tests, build, package
mvn compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:check   # static analysis
```
- **TDD throughout** — RED→GREEN per task; see commit history `T0`→`T9`.
- **CI** (`.github/workflows/ci.yml`): `mvn -B verify` + SpotBugs on every push/PR; GitHub dependency-review (fails on high-severity CVEs) on PRs.
- An adversarial **refutation pass** (fresh agent) and a **live LLM tool-routing eval** are recorded in `docs/implementation/validation-report.md`.

## Security notes
- API key via env var or gitignored `config/` file only; never in source, never in the jar, never sent to the browser.
- web-fetch blocks loopback / private / link-local / IPv6-ULA (`fc00::/7`) / multicast targets and disables redirects.
- Request bodies validated (`@NotBlank`, `@Size(4000)`); tool calls log **name only** (no sensitive args).

## Limitations / next steps
- **No auth / rate limit** on `/api/chat` — it spends OpenAI tokens; fine locally, add a limiter before exposing publicly.
- **DNS-rebinding TOCTOU** residual in web-fetch (validate-then-connect re-resolves) — needs socket-level IP pinning. Documented in `validation-report.md`.
- **Memory is in-process** — lost on restart; the `ChatMemoryStore` seam is ready for a persistent backend.
- Model replies may contain LaTeX (`\( … \)`) rendered as plain text in the minimal UI.
- No streaming (SSE), metrics/tracing, or per-tool timeouts yet.

## Configuration
| Property | Default | Meaning |
|----------|---------|---------|
| `OPENAI_API_KEY` | `not-set` | OpenAI key (env var; overridden by `config/application.properties`) |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | model |
| `spring.ai.openai.chat.options.max-tokens` | `800` | output cap |
