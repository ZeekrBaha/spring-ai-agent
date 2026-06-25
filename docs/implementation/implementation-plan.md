# Implementation Plan — spring-ai-agent

Governed by `constitution.md`. Every task is TDD: RED test first → fail → GREEN → pass → refactor. A task is "done" only when its test went RED→GREEN and full suite is green. Update `progress.md` after every task.

| Task | Goal | Files (likely) | Test first (RED) | Acceptance | Risk / rollback |
|------|------|----------------|------------------|------------|-----------------|
| T0 | Scaffold | `pom.xml`, `Application.java`, `application.yml` | `contextLoads` smoke | App boots, no secrets in yml | low / delete branch |
| T1 | Pin deps | `pom.xml` | build resolves | spring-boot 4.x, `spring-ai-starter-model-openai`, web, validation, junit | dep-version drift → verify via context7 at build |
| T2 | Calculator tool | `tools/CalculatorTool.java` | `CalculatorToolTest`: 2+2=4, `*`/parens, malformed→error | F1 criteria | use a safe expression evaluator, not eval |
| T3 | Time tool | `tools/TimeTool.java` | `TimeToolTest`: valid zone, bad zone→error | F2 | inject a `Clock` for deterministic test |
| T4 | Weather tool | `tools/WeatherTool.java` | `WeatherToolTest`: mock Open-Meteo client, city→temp; not-found→error | F3 | network mocked; real call only in manual smoke |
| T5 | Web-fetch tool + SSRF guard | `tools/WebFetchTool.java` | `WebFetchToolTest`: blocks localhost/127.0.0.1/private IP/ftp; caps size; happy path mocked | F4 + N1 | SSRF is security-critical — test the block list first |
| T6 | Agent service + tool-loop + toolsUsed capture | `agent/AgentService.java`, `config/ChatClientConfig.java` | `AgentServiceTest` with mock `ChatModel`/fake tool-call: asserts correct tool invoked + `toolsUsed` populated | F5 | spike `toolsUsed` capture (observation hook vs wrapper) |
| T6b | Tool registration startup test | `ToolRegistrationTest` | integration: each tool discoverable/callable via client | mitigates #5134 | if fails, register tools explicitly |
| T7 | REST endpoint + memory | `web/ChatController.java`, `dto/*`, `agent/ChatMemoryStore.java` | `ChatControllerTest` (MockMvc, mocked service): 200 happy, 400 blank message; memory keeps 2-turn context | F6,F7 | validation on DTO |
| T8 | Chat UI | `static/index.html`, `app.js`, `styles.css` | manual + Anti-Slop Visual Gate | F8, all 4 states, tokens applied | no framework — vanilla, keep simple |
| T9 | Full validation + refutation | — | whole suite + browser eval cases + fresh-agent refutation | `validation-report.md` filled | — |

## Sequencing
T0→T1 first. T2–T5 parallel-able (independent tools). T6 needs tools. T7 needs T6. T8 needs T7. T9 last.

## Token block
UI work (T8) MUST paste the `design-system.md` token block verbatim.
