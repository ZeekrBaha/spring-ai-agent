package com.baha.agent.config;

import com.baha.agent.tools.CalculatorTool;
import com.baha.agent.tools.TimeTool;
import com.baha.agent.tools.WeatherTool;
import com.baha.agent.tools.WebFetchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant with tools for math, telling the current
            time in a timezone, current weather, and fetching public web pages.
            Use a tool whenever it helps answer accurately. Never fabricate a tool
            result. If a tool returns an error, tell the user plainly.""";

    /**
     * The raw (unwrapped) tool callbacks for the 4 @Tool beans. AgentService
     * wraps these per-call in a RecordingToolCallback bound to a fresh
     * ToolCallSink (reactive-safe capture); the MCP server exposes them as-is.
     * Building explicit callbacks (not bean scanning) also sidesteps spring-ai#5134.
     */
    @Bean
    public AgentTools agentTools(CalculatorTool calculator, TimeTool time,
                                 WeatherTool weather, WebFetchTool webFetch) {
        ToolCallback[] raw = ToolCallbacks.from(calculator, time, weather, webFetch);
        return new AgentTools(List.of(raw));
    }

    /**
     * ChatClient with the system prompt only. Tool callbacks are supplied
     * per-request by AgentService (so each turn gets its own recording sink),
     * never as defaults — that keeps capture call-scoped and reactive-safe.
     */
    @Bean
    public ChatClient agentChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
