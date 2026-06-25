# Architecture — spring-ai-agent

Governed by `constitution.md`. **Architecture is the prompt** — readable from the outside, no hidden state.

## Dependency direction (lower never depends on higher)
```
Config (app.yml, beans)  →  Tools (@Tool beans)  →  AgentService (ChatClient)  →  Controller (REST)  →  UI (static)
```
Tools never import the service/controller. The controller never reaches into a tool directly — only through the model loop.

## Sinks over pipes
- **Calculator, Time** = pure sinks: input → return value, zero side effects.
- **Weather, WebFetch** = sinks with ONE declared network boundary each (outbound HTTP). No queues, no events, no downstream cascade. Effect is visible in the return value.
- The tool-call loop is owned entirely by Spring AI's `ToolCallingAdvisor` — we do not hand-roll loop control.

## Package layout (progressive disclosure)
```
com.baha.agent
├─ config/
│   └─ ChatClientConfig.java      # builds ChatClient, registers tools
├─ tools/
│   ├─ CalculatorTool.java        # @Component, @Tool calculate(...)
│   ├─ TimeTool.java              # @Component, @Tool currentTime(zone)
│   ├─ WeatherTool.java           # @Component, @Tool weather(city)
│   └─ WebFetchTool.java          # @Component, @Tool fetch(url)
├─ agent/
│   ├─ AgentService.java          # wraps ChatClient.prompt()...call()
│   └─ ChatMemoryStore.java       # bounded in-memory per conversationId
└─ web/
    ├─ ChatController.java        # POST /api/chat
    └─ dto/ ChatRequest, ChatResponse
resources/
├─ application.yml                # model props, NO secrets
└─ static/ index.html, app.js, styles.css
```

## Data flow (POST /api/chat)
1. Controller validates `ChatRequest` (non-blank message, message length cap).
2. `AgentService.chat(message, conversationId)` loads memory, calls `ChatClient.prompt().user(message).advisors(memory).call()`.
3. Spring AI runs tool-call loop: model emits tool calls → `ToolCallback` invokes the matching `@Tool` bean method → result fed back → repeat until final text.
4. Service records `toolsUsed` (captured via a logging/observation hook on tool invocation), updates memory, returns `ChatResponse{reply, toolsUsed}`.

## Security boundaries
- `OPENAI_API_KEY` read from env by the OpenAI starter; never logged.
- WebFetchTool: scheme allowlist (http/https), DNS-resolve + reject private/loopback/link-local ranges, response size cap, timeout.
- Request validation via Jakarta Bean Validation on DTO.

## Provider-swap seam (designed, not built)
Swapping OpenAI→Ollama/Claude = swap starter dep + change `spring.ai.<provider>` props. `AgentService` and tools are provider-agnostic (depend only on `ChatClient`).

## Known-issue mitigation
spring-ai#5134 (`defaultTools()` startup detection): `ChatClientConfig` registers tool beans explicitly; integration test `ToolRegistrationTest` asserts each tool is callable through the client at startup.
