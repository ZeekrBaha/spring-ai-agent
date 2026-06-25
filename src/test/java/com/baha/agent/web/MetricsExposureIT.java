package com.baha.agent.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * The Prometheus scrape endpoint is exposed and includes our custom counters;
 * sensitive actuator endpoints are NOT exposed.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureObservability // Boot disables metrics export in tests by default
class MetricsExposureIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void prometheusEndpointExposesCustomChatCounter() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // chatRequests counter is registered when AgentService is built, so it
        // appears in the scrape even before the first request.
        assertThat(response.getBody()).contains("agent_chat_requests");
    }

    @Test
    void sensitiveEndpointsAreNotExposed() {
        assertThat(rest.getForEntity("/actuator/env", String.class).getStatusCode().value())
                .isNotEqualTo(200);
        assertThat(rest.getForEntity("/actuator/beans", String.class).getStatusCode().value())
                .isNotEqualTo(200);
    }
}
