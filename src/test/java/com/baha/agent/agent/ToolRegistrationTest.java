package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Startup integration test: asserts every @Tool bean is registered as a
 * callback at boot. Mitigates spring-ai#5134 (defaultTools startup detection).
 */
@SpringBootTest
class ToolRegistrationTest {

    @Autowired
    AgentTools agentTools;

    @Test
    void allFourToolsRegistered() {
        assertThat(agentTools.callbacks().stream()
                .map(c -> c.getToolDefinition().name()).toList())
                .containsExactlyInAnyOrder("calculate", "currentTime", "weather", "fetchUrl");
    }
}
