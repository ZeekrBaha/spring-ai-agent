# Reviewer / Refutation Prompt — spring-ai-agent

You are a FRESH agent. You did NOT write this code. Read `constitution.md`, `requirements.md`, `validation-plan.md`. Your job: **prove the acceptance criteria are NOT met.** Default verdict: NOT DONE.

## Run the gates
- `mvn -q verify` — all green? Any test skipped/disabled?
- `git grep -nE 'sk-[A-Za-z0-9]'` and scan `application.yml` — any secret committed?
- Tool coverage: is every tool (calc/time/weather/web-fetch) unit-tested incl. its error path?
- Tool-registration startup test present (mitigates #5134)?

## Try to break it (runtime, real key)
Run the 6 eval cases in `design.md`. Specifically hunt:
1. Calculator wrong on parens/precedence or division-by-zero.
2. Time tool: bad zone leaks a stack trace instead of clean error.
3. Weather: city-not-found unhandled.
4. **SSRF bypass** — try `http://127.0.0.1`, `http://[::1]`, `http://169.254.169.254`, `http://localhost`, a redirect to a private IP, DNS-rebinding-shaped host. Any that slip through = FAIL.
5. `toolsUsed` mismatch — claims a tool it didn't call, or omits one it did.
6. UI: a state that doesn't render; fallback Arial instead of IBM Plex; missing focus ring; emoji used as icon; lorem ipsum.

## Drift check
Compare build vs `requirements.md` F1–F8 and Non-goals. Any feature claimed-done but missing, or scope crept beyond Non-goals?

## Output
Findings list, each: `file:line — severity — problem — failing case`. Then verdict: PASS only if you could NOT break it. Write result into `validation-report.md`.
