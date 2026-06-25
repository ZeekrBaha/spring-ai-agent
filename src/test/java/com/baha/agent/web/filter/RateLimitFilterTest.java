package com.baha.agent.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter(int capacity) {
        return new RateLimitFilter(capacity, capacity, Duration.ofMinutes(1), false);
    }

    private RateLimitFilter trustingProxyFilter(int capacity) {
        return new RateLimitFilter(capacity, capacity, Duration.ofMinutes(1), true);
    }

    private MockHttpServletResponse pass(RateLimitFilter f, String ip, String path) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void allowsUpToCapacityThenReturns429WithRetryAfter() throws Exception {
        RateLimitFilter f = filter(2);

        assertThat(pass(f, "1.1.1.1", "/api/chat").getStatus()).isEqualTo(200);
        assertThat(pass(f, "1.1.1.1", "/api/chat").getStatus()).isEqualTo(200);

        MockHttpServletResponse blocked = pass(f, "1.1.1.1", "/api/chat");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void differentIpsHaveIndependentBuckets() throws Exception {
        RateLimitFilter f = filter(1);

        assertThat(pass(f, "1.1.1.1", "/api/chat").getStatus()).isEqualTo(200);
        assertThat(pass(f, "2.2.2.2", "/api/chat").getStatus()).isEqualTo(200);
        // First IP is now exhausted, second IP unaffected.
        assertThat(pass(f, "1.1.1.1", "/api/chat").getStatus()).isEqualTo(429);
        assertThat(pass(f, "2.2.2.2", "/api/chat").getStatus()).isEqualTo(429);
    }

    @Test
    void alsoLimitsStreamEndpoint() throws Exception {
        RateLimitFilter f = filter(1);
        assertThat(pass(f, "1.1.1.1", "/api/chat/stream").getStatus()).isEqualTo(200);
        assertThat(pass(f, "1.1.1.1", "/api/chat/stream").getStatus()).isEqualTo(429);
    }

    @Test
    void doesNotLimitNonChatPaths() throws Exception {
        RateLimitFilter f = filter(1);
        for (int i = 0; i < 5; i++) {
            assertThat(pass(f, "1.1.1.1", "/actuator/health").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void ignoresForwardedForByDefaultToPreventSpoofedBypass() throws Exception {
        // Default (no trusted proxy): X-Forwarded-For must NOT be honored, else a
        // client could mint a fresh bucket per request by spoofing the header.
        RateLimitFilter f = filter(1);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/chat");
        req1.setRemoteAddr("198.51.100.5");
        req1.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        f.doFilter(req1, res1, new MockFilterChain());
        assertThat(res1.getStatus()).isEqualTo(200);

        // Same real client (same remoteAddr), different spoofed XFF -> still limited.
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/chat");
        req2.setRemoteAddr("198.51.100.5");
        req2.addHeader("X-Forwarded-For", "8.8.8.8");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        f.doFilter(req2, res2, new MockFilterChain());
        assertThat(res2.getStatus()).isEqualTo(429);
    }

    @Test
    void usesForwardedForOnlyWhenProxyTrustEnabled() throws Exception {
        RateLimitFilter f = trustingProxyFilter(1);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/chat");
        req1.setRemoteAddr("10.0.0.1"); // proxy
        req1.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        f.doFilter(req1, res1, new MockFilterChain());
        assertThat(res1.getStatus()).isEqualTo(200);

        // Same client (same first XFF hop) via a different proxy address -> limited.
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/chat");
        req2.setRemoteAddr("10.0.0.2");
        req2.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        f.doFilter(req2, res2, new MockFilterChain());
        assertThat(res2.getStatus()).isEqualTo(429);
    }
}
