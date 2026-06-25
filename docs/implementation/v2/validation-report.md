# Validation Report — spring-ai-agent v2

Date: 2026-06-25. Branch: `feat/v2-extensions`.

## Automated gates
| Check | Command | Result |
|-------|---------|--------|
| Unit | `mvn -B test` | **57 passed**, 0 failed/skipped |
| Integration (Testcontainers) | `mvn -B verify` (failsafe) | **8 passed** (Jdbc store, memory wiring, metrics exposure) |
| Static analysis | `mvn -B spotbugs:check` | clean (documented excludes only) |
| Secret scan | `git grep 'sk-…'` + scan yml/compose | none; compose uses env-interpolated local defaults |

## Runtime verification (observed, not assumed)
- **SSE streaming:** `curl -N POST /api/chat/stream` emits incremental `token` events then `done`; chat UI fills token-by-token; weather query rendered streamed reply + `weather` chip (`docs/screenshots/streaming.png`).
- **Persistence:** Testcontainers real Postgres — append/history/bounded-prune/role-roundtrip/**survives-new-instance** all pass; `postgres` profile selects `JdbcChatMemoryStore` (`MemoryWiringPostgresIT`); app boots with **no** DB by default (`MemoryWiringTest`).
- **Rate limit:** filter returns 429 + `Retry-After` past capacity; independent per remote IP; limits `/api/chat/stream`; non-chat paths unlimited.
- **Metrics:** `/actuator/prometheus` → 200, exposes `agent_chat_requests` + `agent_tool_invocations`; `/actuator/env` and `/actuator/beans` return non-200.
- **MCP HTTP/SSE:** startup logs `Registered tools: 4`; `/sse` → 200 `text/event-stream`.
- **MCP stdio:** `-Dspring.profiles.active=mcp-stdio` — stdout is pure JSON-RPC (no banner/log; logs on stderr); `tools/list` returns `calculate, currentTime, weather, fetchUrl`.

## Refutation pass (grader ≠ doer)
A fresh adversarial agent attempted to disprove the acceptance criteria. Verdict: **FAIL — 1 high, 3 medium**. All actionable findings resolved:

| Sev | Finding | Resolution |
|-----|---------|------------|
| HIGH | Rate limit bypassable by spoofing `X-Forwarded-For` (each fake IP → fresh bucket) in the default no-proxy run mode | XFF now honored **only** when `agent.ratelimit.trust-forwarded-for=true`; default keys on `getRemoteAddr()`. Tests `ignoresForwardedForByDefault…`, `usesForwardedForOnlyWhenProxyTrustEnabled`. |
| MED | `buckets.clear()` soft-cap reset everyone's limit under flood (+ check-then-act race) | Replaced with an access-ordered LRU (`LinkedHashMap` + `removeEldestEntry`); evicts only the least-recently-used entry. |
| MED | A2 streaming-capture never exercised with a real tool firing on another thread | Added `perCallStreamingCallbacksCaptureToolFiredOnAnotherThread` — invokes AgentService's actual per-call callbacks from a worker thread, asserts the sink captured `calculate` (would fail under ThreadLocal). |
| MED | D2/D3 (MCP HTTP round-trip + stdio cleanliness) verified only manually | Recorded the runtime evidence above; left as documented runtime checks (full in-test MCP client round-trip deferred). |

Refuter explicitly **could not** reproduce the R1 reactive-leak hypothesis (per-call sink is correct), and confirmed: no SQL injection in `JdbcChatMemoryStore`, correct prune, actuator not leaking, app boots without DB/key, no disabled/skipped tests.

## Residual / known limitations
- **Rate limit is single-node, in-memory** (non-goal: Redis-distributed). Behind a proxy, set `trust-forwarded-for=true` only with a trusted proxy stripping client XFF.
- **JDBC `seq` cross-instance race:** instance-scoped `synchronized` + non-transactional read-MAX/insert could collide on PK across multiple nodes (horizontal memory scaling is a non-goal).
- **MCP endpoint unauthenticated** (same posture as `/api/chat`) — local use only.
- **DNS-rebinding TOCTOU** residual in web-fetch (carried from v1).

## Verdict
All four features (SSE streaming, Postgres memory, rate limit + metrics, MCP) implemented TDD; gates green; runtime-verified; refutation findings resolved. **Ship-ready as MVP.**
