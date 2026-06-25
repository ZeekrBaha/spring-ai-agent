# Validation Plan — spring-ai-agent

Governed by `constitution.md`. Defined BEFORE coding.

## Automated gates (must be green before "done")
| Check | Command | Pass condition |
|-------|---------|----------------|
| Compile | `mvn -q compile` | no errors |
| Unit + integration | `mvn -q test` | all green; every tool + controller + tool-registration covered |
| Full verify | `mvn -q verify` | build + tests pass |
| No secret in repo | `git grep -nE 'sk-[A-Za-z0-9]'` + scan yml | zero matches |

## Runtime verification (NOT compile-clean alone)
Run app with a real `OPENAI_API_KEY`, open the chat page, execute the 6 eval cases from `design.md`:
1. Math → calculator only, correct number.
2. Timezone → time only.
3. Weather in a city → weather tool, plausible current temp.
4. Compound (math + time) → both tools fire (check `toolsUsed`).
5. `fetch http://localhost:8080` → SSRF-blocked, friendly error (NOT a stack trace).
6. Chit-chat → no tool, direct reply.

## Anti-Slop Visual Gate (UI)
- IBM Plex Sans/Mono actually loaded (not fallback Arial); teal accent present; dark backdrop (not #fff).
- All 4 states render with real data.
- Contrast AA, visible focus ring, Enter sends / Shift+Enter newline, icon buttons have `aria-label`.
- No emoji-as-icons, no gradients, no lorem ipsum.

## LLM test discipline
- Never assert exact LLM prose. Assert: which tool was invoked, structured fields, and error handling. Use a mock/fake `ChatModel` in unit tests; reserve the real model for manual runtime eval.

## Refutation pass (grader ≠ doer)
A FRESH agent/session is instructed: "Prove the acceptance criteria are NOT met." Hunt for: a tool that returns wrong results, an unhandled error state, SSRF bypass, `toolsUsed` mismatch, a UI state that doesn't render, spec-vs-build drift. Record outcome in `validation-report.md`. Default "not done" until refutation can't break it.

## Cost note
Document approximate per-request token cost and the configured output-token cap.
