package com.baha.agent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    @Test
    void returnsReplyAndEmptyToolsWhenNoToolUsed() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().user(anyString()).call().content()).thenReturn("Hello there.");

        AgentService service = new AgentService(client, new ToolCallRecorder());
        ChatResult result = service.chat("hi");

        assertThat(result.reply()).isEqualTo("Hello there.");
        assertThat(result.toolsUsed()).isEmpty();
    }
}
