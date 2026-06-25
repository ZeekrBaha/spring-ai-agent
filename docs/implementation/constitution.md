# Constitution — spring-ai-agent

Immutable principles. Every downstream doc and prompt references this file. If any doc conflicts with this, **this wins**.

Inherited from `~/.claude/CLAUDE.md` (Karpathy rules + mandatory TDD) and restated here.

## Hard rules (verbatim into every prompt)

1. **Test-first (Iron Law).** No production code without a failing test first. RED → watch it fail for the right reason → GREEN minimal code → watch it pass → REFACTOR. Code written before its test is deleted and restarted. A test that passes the moment written proves nothing.
2. **Security from line one.** `OPENAI_API_KEY` lives in an env var, never in source, `application.yml`, or git. No secret reaches the browser/JS. Tool inputs are validated at the boundary.
3. **Anti-slop.** Pin design tokens before screens. Banned: Inter/Roboto/Arial, purple/indigo→blue gradients, gradient text, emoji-as-icons, hero→3-cards→testimonials template, lorem ipsum. Real data only.
4. **No invented business rules.** Unknown behavior is marked `ASSUMPTION` or escalated — never silently guessed.
5. **Verify in runtime, not compile-clean.** Done means: suite green AND app runs AND a real chat in the browser routes to the right tool. Build success alone is never "done".
6. **Simplicity / minimal change.** Simplest code that passes acceptance criteria. No speculative abstraction, no defensive bloat, no drive-by refactors.
7. **Grader ≠ doer.** Before "done", a fresh refutation pass tries to prove acceptance criteria are NOT met. Default to "not done" until refutation fails to break it.

## Scope guard
MVP = utility-tool agent over REST + a minimal chat page. Anything beyond Section "Non-goals" in `requirements.md` is out of scope until explicitly approved.
