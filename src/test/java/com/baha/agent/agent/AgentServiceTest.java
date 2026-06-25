package com.baha.agent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    @Test
    void returnsReplyAndEmptyToolsWhenNoToolUsed() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().messages(anyList()).user(anyString()).call().content())
                .thenReturn("Hello there.");

        AgentService service = new AgentService(client, new ToolCallRecorder(), new ChatMemoryStore(20));
        ChatResult result = service.chat("hi", "conv-1");

        assertThat(result.reply()).isEqualTo("Hello there.");
        assertThat(result.toolsUsed()).isEmpty();
    }

    @Test
    void modelCallFailureIsWrappedAsUpstreamException() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().messages(anyList()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("openai unreachable"));

        AgentService service = new AgentService(client, new ToolCallRecorder(), new ChatMemoryStore(20));

        assertThatThrownBy(() -> service.chat("hi", "conv-1"))
                .isInstanceOf(AgentUpstreamException.class);
    }

    @Test
    void nullModelContentBecomesEmptyReplyNotNpe() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().messages(anyList()).user(anyString()).call().content())
                .thenReturn(null);
        ChatMemoryStore memory = new ChatMemoryStore(20);

        AgentService service = new AgentService(client, new ToolCallRecorder(), memory);
        ChatResult result = service.chat("hi", "conv-1");

        assertThat(result.reply()).isEmpty();
        assertThat(memory.history("conv-1")).extracting(m -> m.getText())
                .containsExactly("hi", "");
    }

    @Test
    void persistsTurnToMemory() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().messages(anyList()).user(anyString()).call().content())
                .thenReturn("reply");
        ChatMemoryStore memory = new ChatMemoryStore(20);

        AgentService service = new AgentService(client, new ToolCallRecorder(), memory);
        service.chat("question", "conv-1");

        assertThat(memory.history("conv-1"))
                .extracting(m -> m.getText())
                .containsExactly("question", "reply");
    }
}
