package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import com.baha.agent.tools.CalculatorTool;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsTest {

    @Test
    void chatRequestCounterIncrementsPerTurn() {
        MeterRegistry registry = new SimpleMeterRegistry();
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().toolCallbacks(anyList()).messages(anyList())
                .user(anyString()).call().content()).thenReturn("hi");

        AgentService service = new AgentService(client, new AgentTools(List.of()),
                new InMemoryChatMemoryStore(20), registry);
        service.chat("q", "c1");

        assertThat(registry.get("agent.chat.requests").counter().count()).isEqualTo(1.0);
    }

    @Test
    void toolInvocationCounterTaggedByToolName() {
        MeterRegistry registry = new SimpleMeterRegistry();
        ToolCallback raw = ToolCallbacks.from(new CalculatorTool())[0];
        RecordingToolCallback cb = new RecordingToolCallback(raw, new ToolCallSink(), registry);

        cb.call("{\"expression\":\"2 + 2\"}");

        assertThat(registry.get("agent.tool.invocations").tag("tool", "calculate")
                .counter().count()).isEqualTo(1.0);
    }
}
