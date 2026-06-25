package com.baha.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The MCP server must expose exactly the four @Tool beans, and a tool invoked
 * through the MCP provider must return the same result as the in-app tool
 * (parity). Booting this context also proves the MCP webmvc auto-configuration
 * coexists with the rest of the app.
 */
@SpringBootTest
class McpToolsTest {

    @Autowired
    @Qualifier("mcpAgentTools")
    ToolCallbackProvider mcpTools;

    @Test
    void exposesExactlyTheFourAgentTools() {
        assertThat(Arrays.stream(mcpTools.getToolCallbacks())
                .map(cb -> cb.getToolDefinition().name()))
                .containsExactlyInAnyOrder("calculate", "currentTime", "weather", "fetchUrl");
    }

    @Test
    void calculatorOverMcpMatchesInAppResult() {
        ToolCallback calculate = Arrays.stream(mcpTools.getToolCallbacks())
                .filter(cb -> cb.getToolDefinition().name().equals("calculate"))
                .findFirst().orElseThrow();

        assertThat(calculate.call("{\"expression\":\"2 + 2\"}")).isEqualTo("4");
    }
}
