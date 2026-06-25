package com.baha.agent.config;

import com.baha.agent.tools.CalculatorTool;
import com.baha.agent.tools.TimeTool;
import com.baha.agent.tools.WeatherTool;
import com.baha.agent.tools.WebFetchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the four @Tool beans to MCP clients (Claude Desktop, Cursor, …) over
 * the MCP server's HTTP/SSE transport. The MCP server auto-configuration
 * discovers this {@link ToolCallbackProvider} and registers its tools.
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider mcpAgentTools(CalculatorTool calculator, TimeTool time,
                                              WeatherTool weather, WebFetchTool webFetch) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calculator, time, weather, webFetch)
                .build();
    }
}
