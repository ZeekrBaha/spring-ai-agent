package com.baha.agent.config;

import com.baha.agent.agent.RecordingToolCallback;
import com.baha.agent.agent.ToolCallRecorder;
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

import java.util.Arrays;
import java.util.List;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant with tools for math, telling the current
            time in a timezone, current weather, and fetching public web pages.
            Use a tool whenever it helps answer accurately. Never fabricate a tool
            result. If a tool returns an error, tell the user plainly.""";

    /**
     * Wrap each @Tool bean's callback in a RecordingToolCallback. Registering
     * explicit callbacks (rather than relying on bean scanning) also sidesteps
     * spring-ai#5134.
     */
    @Bean
    public AgentTools agentTools(CalculatorTool calculator, TimeTool time,
                                 WeatherTool weather, WebFetchTool webFetch,
                                 ToolCallRecorder recorder) {
        ToolCallback[] raw = ToolCallbacks.from(calculator, time, weather, webFetch);
        List<ToolCallback> recording = Arrays.stream(raw)
                .map(cb -> (ToolCallback) new RecordingToolCallback(cb, recorder))
                .toList();
        return new AgentTools(recording);
    }

    @Bean
    public ChatClient agentChatClient(ChatModel chatModel, AgentTools agentTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(agentTools.callbacks().toArray(new ToolCallback[0]))
                .build();
    }
}
