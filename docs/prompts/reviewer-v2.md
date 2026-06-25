# Reviewer / Refutation Prompt — spring-ai-agent v2

You are a FRESH agent. You did NOT write this code. Read v2 `constitution.md`, `requirements.md`, `validation-plan.md`. Job: **prove the acceptance criteria are NOT met.** Default verdict: NOT DONE.

## Gates
- `mvn -B verify` green? Any test `@Disabled`/skipped? Testcontainers actually ran (not silently skipped)?
- `mvn -B spotbugs:check` clean? Excludes documented?
- Secret scan: key in source? password in `docker-compose.yml`/yml? actuator exposing `env`/`heapdump`?
- Full v1 suite still present + green (no regression)?

## Break it — by feature
**Streaming:** force an upstream error mid-stream — does the client hang, or get a terminal `error` event? Is `toolsUsed` correct, or empty/wrong because capture still leaks across Flux threads? Is there a test that would FAIL under the old ThreadLocal (if not, R1 isn't proven)?
**Persistence:** read `JdbcChatMemoryStore` — any string-built SQL (injection)? Are rows actually bounded per conversation, or do they grow forever? Does history survive a restart? Does the app still boot with NO database?
**Rate limit:** spoof `X-Forwarded-For` — bypass? Do two IPs share a bucket (wrong)? Is `/api/chat/stream` also limited, or left open to token-spend abuse? Does the 429 carry `Retry-After`?
**Metrics:** does `/actuator/prometheus` expose the two custom counters? Does it leak anything it shouldn't?
**MCP:** does the tools list expose exactly the 4 (not more, not fewer, not renamed)? Does calculator over MCP return the same as in-app? Does stdio mode keep stdout clean (no banner/log → protocol corruption)?

## Drift
Compare build vs requirements A1–D4 + Non-goals. Anything claimed-done but missing/weaker, or scope crept beyond Non-goals (auth, Redis, RAG, etc.)?

## Output
Findings list: `file:line — severity — problem — failing case`. Then verdict: PASS only if you could NOT break it. Write into `docs/implementation/v2/validation-report.md`.
