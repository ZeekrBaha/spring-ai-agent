package com.baha.agent.web.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-client token-bucket rate limit on the chat endpoints ({@code /api/chat}
 * and {@code /api/chat/stream}), so a single caller cannot drain OpenAI tokens.
 * Over the limit returns {@code 429} with a {@code Retry-After} header.
 *
 * <p>Client key = the remote address. {@code X-Forwarded-For} is honored ONLY
 * when {@code agent.ratelimit.trust-forwarded-for=true} (i.e. you run behind a
 * trusted reverse proxy) — otherwise a client could spoof the header to mint a
 * fresh bucket per request and bypass the limit entirely.
 *
 * <p>In-memory and per-instance (single-node MVP; distributed limiting is a
 * non-goal). The bucket map is an access-ordered LRU bounded at
 * {@code MAX_TRACKED_CLIENTS}; eviction drops only the least-recently-used
 * entry (never resets everyone's limit).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 100_000;

    private final int capacity;
    private final long refillTokens;
    private final Duration refillPeriod;
    private final boolean trustForwardedFor;
    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_TRACKED_CLIENTS;
                }
            });

    @Autowired
    public RateLimitFilter(
            @Value("${agent.ratelimit.capacity:20}") int capacity,
            @Value("${agent.ratelimit.refill-tokens:20}") long refillTokens,
            @Value("${agent.ratelimit.refill-period:PT1M}") Duration refillPeriod,
            @Value("${agent.ratelimit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = refillPeriod;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isRateLimited(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = bucketFor(clientKey(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setStatus(429); // HTTP 429 Too Many Requests (no jakarta constant)
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again shortly.\"}");
    }

    private boolean isRateLimited(String path) {
        return path != null && path.startsWith("/api/chat");
    }

    private Bucket bucketFor(String key) {
        // Access-ordered LRU evicts only the eldest entry past the cap.
        return buckets.computeIfAbsent(key, k -> newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, refillPeriod)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * The remote address by default; the first X-Forwarded-For hop only when a
     * trusted proxy is configured (otherwise the header is spoofable).
     */
    private String clientKey(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
