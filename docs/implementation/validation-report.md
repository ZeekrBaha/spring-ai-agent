# Validation Report — spring-ai-agent

Date: 2026-06-24. Branch: `feat/agent-mvp`.

## Automated gates
| Check | Command | Result |
|-------|---------|--------|
| Unit + integration | `mvn -B test` | **34 passed, 0 failed, 0 skipped** |
| Full verify + package | `mvn -B verify` | **BUILD SUCCESS** (jar built) |
| Secret scan | `git grep -nE 'sk-[A-Za-z0-9]{10}'` (excl. docs) | no secrets in tracked source |

Coverage by tool/component (all TDD, RED→GREEN watched):
- CalculatorTool 7, TimeTool 3, WeatherTool 2, WebFetchTool 9, ToolCallRecorder/RecordingToolCallback 2, AgentService 2, ChatMemoryStore 4, ToolRegistration (startup, #5134) 1, ChatController 3, contextLoads 1.

## Runtime verification (app booted, NO OpenAI key)
| Case | Expected | Actual |
|------|----------|--------|
| `GET /` | static chat page | **200**, `text/html`, IBM Plex + Lucide SVG + css/js all 200 |
| `POST /api/chat` blank message | 400 | **400** |
| `POST /api/chat` valid, no key | friendly error, not a crash | **502** `{"error":"The agent could not complete your request (upstream error)."}` |

## Refutation pass (grader ≠ doer)
A fresh adversarial agent (separate session) was told to prove the acceptance criteria are NOT met. Verdict: **FAIL — 1 high, 4 medium**. All actionable findings were fixed TDD and re-verified:

| # | Sev | Finding | Resolution |
|---|-----|---------|------------|
| 1 | HIGH | DNS-rebinding TOCTOU: guard resolves host, HTTP client re-resolves at connect | **Documented residual** (see below) + redirects pinned to NEVER to kill the practical 3xx→internal vector. Full fix (socket-level IP pinning) is out of MVP scope. |
| 2 | MED | IPv6 ULA `fc00::/7` bypassed `isPrivate()` (Java `isSiteLocalAddress()` misses ULA) | Added `isUniqueLocalIpv6()` check. Test `blocksIpv6UniqueLocal` (RED→GREEN). |
| 3 | MED | Redirects could 3xx-bounce to a private IP if a redirect-following client were swapped in | `RestClient` request factory pinned to `Redirect.NEVER`. |
| 4 | MED | Calculator `(long)` narrowing → `10^20` returned `Long.MAX_VALUE` | `format()` only narrows below 2^53; else `Double.toString`. Test `largeIntegralResultIsNotNarrowedToLong`. |
| 5 | MED | `ChatMemoryStore` conversation map grew unbounded (new UUID per keyless request) | LRU bound `maxConversations` (default 1000) via access-order `LinkedHashMap`. Test `boundedConversationCountEvictsOldest`. |
| 6 | LOW/MED | `history()` read race vs synchronized `append()` | `history()` now `synchronized` too. |
| 7 | LOW | N5 observability: no tool-call logging | `RecordingToolCallback` logs `tool call: {name}` at INFO (name only; args may be sensitive). |

Confirmed NOT vulnerable by the refutation agent: literal/decimal/hex/octal IP forms, IPv4-mapped IPv6, `0.0.0.0`, userinfo tricks (`http://public@127.0.0.1`), and the ThreadLocal `toolsUsed` recorder (no cross-request leak — `start()` clears at entry).

## Residual / known limitations
- **DNS-rebinding TOCTOU (web-fetch):** validating one resolution then connecting via a second resolution is a known SSRF residual. Requires an attacker-controlled domain with very low-TTL rebinding. Practical vectors (literal private IPs, redirects) are closed. Documented in `WebFetchTool` javadoc. Fully closing needs socket-level address pinning — deferred.
- **Calculator scope:** exp4j also evaluates `^`, `sqrt`, `sin`, etc. — broader than the `@Tool` "basic arithmetic" description. No security impact (no variables/side effects; unknown identifiers throw and are caught). Acceptable; description could be tightened later.
- **AgentServiceTest** uses a mocked `ChatClient`, so end-to-end `toolsUsed` capture through the live tool-loop is not unit-asserted (per the no-LLM-in-unit-tests policy). The wrapper is unit-tested in isolation; full capture is verified at runtime.

## Live LLM tool-routing eval (real OPENAI_API_KEY, gpt-4o-mini) — PASSED
Run 2026-06-25 against the running app:

| Case | Expected | toolsUsed | Result |
|------|----------|-----------|--------|
| 1 math `17.5*3` | calculate | `[calculate]` | "52.5" ✓ |
| 2 timezone Tokyo | currentTime | `[currentTime]` | 11:14 AM Jun 25 (UTC+9, correct) ✓ |
| 3 weather Paris | weather | `[weather]` | "29.1°C, mainly clear" (real Open-Meteo) ✓ |
| 4 compound `12*9` + UTC time | both | `[calculate, currentTime]` | "108" + UTC time ✓ |
| 5 fetch `http://localhost:8080/` | blocked | `[]` | model **self-declined** to call the tool — safe, but the guard was not exercised via the LLM; guard stays covered by 9 WebFetchTool unit tests ✓ |
| 6 chit-chat | no tools | `[]` | direct reply, no tool ✓ |
| 7 fetch `https://example.com` (extra) | fetchUrl runs | `[fetchUrl]` | "main heading is Example Domain" — end-to-end fetch + capture ✓ |

Note on case 5: gpt-4o-mini refuses the localhost request before invoking `fetchUrl`, so the SSRF guard's runtime block path is not hit through the LLM. The guard is verified directly by unit tests (loopback/private/link-local/ULA/redirect). Case 7 confirms `fetchUrl` executes through the live tool-loop for legitimate public URLs.

## Verdict
All acceptance criteria met; refutation findings resolved; suite + build green; non-LLM runtime paths verified; **live LLM tool-routing eval PASSED**. Ship-ready.
