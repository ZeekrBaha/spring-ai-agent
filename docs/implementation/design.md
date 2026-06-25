# Design — spring-ai-agent

Governed by `constitution.md`.

## UX flow
Single chat screen. User types → message appears right-aligned → "thinking" indicator → reply appears left-aligned with a small "tools used: calculator, weather" chip row. Errors render as an inline error bubble, not an alert.

## Screens & states
| State | Render |
|-------|--------|
| Empty | Centered prompt: "Ask me to calculate, tell time, check weather, or fetch a page." + 3 real example chips. |
| Loading | Disabled input + animated dots in an assistant bubble. |
| Success | Assistant bubble + tool chips. |
| Error | Red-bordered assistant bubble with the server error message + retry affordance. |

## API contract
`POST /api/chat`
```json
// request
{ "message": "weather in Paris and what's 12*9?", "conversationId": "optional-uuid" }
// response 200
{ "reply": "It's 14°C and cloudy in Paris. 12 × 9 = 108.", "toolsUsed": ["weather","calculator"], "conversationId": "uuid" }
// response 400 (validation)
{ "error": "message must not be blank" }
```

## Tool specs (model-facing descriptions matter — they drive routing)
| Tool | Signature | `@Tool` description | Errors |
|------|-----------|---------------------|--------|
| calculate | `calculate(String expression)` | "Evaluate a basic arithmetic expression (+ - * / parentheses). Use for any math." | malformed → "Invalid expression" |
| currentTime | `currentTime(String ianaZone)` | "Get the current date and time in an IANA timezone, e.g. 'Asia/Tokyo'." | bad zone → "Unknown timezone" |
| weather | `weather(String city)` | "Get current weather (temp, conditions) for a city name." | not found → "City not found" |
| fetchUrl | `fetchUrl(String url)` | "Fetch readable text from a public http/https web page." | blocked/oversize → clear message |

## AI behavior
- System prompt: "You are a helpful assistant with tools. Use tools for math, time, weather, and fetching web pages. Never fabricate a tool result. If a tool errors, tell the user plainly."
- Fallback: if no tool fits, answer directly.
- Max output tokens capped (config). Memory window bounded (e.g. last 10 turns).

## Eval cases (for validation, not asserting exact prose)
1. Pure math → calculator only.
2. Timezone question → time only.
3. "weather in X" → weather only.
4. Compound (math + time) → both tools called.
5. "fetch http://localhost" → blocked by SSRF guard, friendly error.
6. Off-topic chit-chat → no tool, direct reply.

## Reviewer pass (pre-implementation) — findings
- ✅ Tool descriptions are action-oriented (drives correct routing).
- ✅ SSRF guard specified before build, not bolted on.
- ⚠ LLM-dependent tests must mock the model or assert tool-call records, never exact text — carried into validation-plan.
- ⚠ `toolsUsed` capture mechanism is the one non-obvious bit → spike it in T6 (observation hook vs. wrapping ToolCallback).
