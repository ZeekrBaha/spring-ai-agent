package com.baha.agent.tools;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimeToolTest {

    // Fixed instant: 2026-06-24T12:00:00Z
    private final Clock fixed = Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneOffset.UTC);
    private final TimeTool tool = new TimeTool(fixed);

    @Test
    void returnsTimeInUtc() {
        assertThat(tool.currentTime("UTC")).contains("2026-06-24T12:00");
    }

    @Test
    void appliesTimezoneOffset() {
        // Tokyo is UTC+9 -> 21:00 same day
        assertThat(tool.currentTime("Asia/Tokyo")).contains("2026-06-24T21:00");
    }

    @Test
    void unknownTimezoneReturnsClearError() {
        assertThat(tool.currentTime("Not/AZone")).startsWith("Unknown timezone");
    }
}
