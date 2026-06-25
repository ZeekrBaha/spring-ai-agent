# Requirements — spring-ai-agent

Governed by `constitution.md`.

## Functional

| ID | Requirement | Acceptance criteria | Maps to task |
|----|-------------|---------------------|--------------|
| F1 | Calculator tool | `@Tool` method evaluates a basic arithmetic expression; returns numeric result; rejects malformed input with a clear error. Unit-tested. | T2 |
| F2 | Time tool | `@Tool` method returns current time for a given IANA timezone; invalid zone → clear error. Unit-tested. | T3 |
| F3 | Weather tool | `@Tool` method takes a city, geocodes via Open-Meteo, returns current temp + conditions. Network boundary mocked in tests. | T4 |
| F4 | Web-fetch tool | `@Tool` method GETs a URL, returns capped extracted text. Blocks non-http(s) + private/loopback hosts (SSRF). Unit-tested. | T5 |
| F5 | Agent loop | `ChatClient` with all tools registered; given NL prompt, calls correct tool(s) and composes final answer. | T6 |
| F6 | REST endpoint | `POST /api/chat` `{message, conversationId?}` → `{reply, toolsUsed[]}`. Validated request body. | T7 |
| F7 | Conversation memory | Multi-turn context kept in-memory per `conversationId` (bounded). | T7 |
| F8 | Chat UI | Static page: message list, input, send. Shows which tools were used per reply. Empty/loading/error/success states. | T8 |

## Non-functional
- N1 Security: key in env; no client secret; web-fetch SSRF guard; request-body validation.
- N2 Cost: cap max output tokens + memory window; document.
- N3 Tests: every tool unit-tested (RED-first); controller tested with mocked model; one startup integration test (mitigates #5134).
- N4 Quality gates: `mvn verify` (compile + test) green; no checkstyle/SpotBugs errors if enabled.
- N5 Observability: log each tool call (name + args, secrets redacted) at INFO.

## Non-goals (out of scope, MVP)
- Auth / multi-user / persistence beyond in-memory.
- Streaming responses (SSE) — phase 2.
- Provider swap UI (designed-for, not built).
- RAG / vector store / file upload.
- Production deploy / Docker.
