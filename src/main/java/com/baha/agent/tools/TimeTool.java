package com.baha.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Time tool exposed to the agent. Returns the current date/time for an IANA
 * timezone. Pure sink: input zone -> formatted time string, no side effects.
 * Clock is injected so behavior is deterministic under test.
 */
@Component
public class TimeTool {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Clock clock;

    public TimeTool() {
        this(Clock.systemUTC());
    }

    TimeTool(Clock clock) {
        this.clock = clock;
    }

    @Tool(description = "Get the current date and time in an IANA timezone, e.g. 'Asia/Tokyo' or 'UTC'.")
    public String currentTime(String ianaZone) {
        try {
            ZoneId zone = ZoneId.of(ianaZone);
            String formatted = Instant.now(clock).atZone(zone).format(FORMAT);
            return formatted + " (" + zone.getId() + ")";
        } catch (DateTimeException e) {
            return "Unknown timezone: " + ianaZone;
        }
    }
}
