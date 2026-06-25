package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    private final AgentTools noTools = new AgentTools(List.of());

    private AgentService serviceWith(ChatClient client, ChatMemoryStore memory) {
        return new AgentService(client, noTools, memory, new SimpleMeterRegistry());
    }

    private ChatClient mockReplying(String content) {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().toolCallbacks(anyList()).messages(anyList())
                .user(anyString()).call().content())
                .thenReturn(content);
        return client;
    }

    @Test
    void returnsReplyAndEmptyToolsWhenNoToolUsed() {
        AgentService service = serviceWith(mockReplying("Hello there."), new InMemoryChatMemoryStore(20));

        ChatResult result = service.chat("hi", "conv-1");

        assertThat(result.reply()).isEqualTo("Hello there.");
        assertThat(result.toolsUsed()).isEmpty();
    }

    @Test
    void modelCallFailureIsWrappedAsUpstreamException() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().toolCallbacks(anyList()).messages(anyList())
                .user(anyString()).call().content())
                .thenThrow(new RuntimeException("openai unreachable"));

        AgentService service = new AgentService(client, noTools, new InMemoryChatMemoryStore(20),
                new SimpleMeterRegistry());

        assertThatThrownBy(() -> service.chat("hi", "conv-1"))
                .isInstanceOf(AgentUpstreamException.class);
    }

    @Test
    void nullModelContentBecomesEmptyReplyNotNpe() {
        ChatMemoryStore memory = new InMemoryChatMemoryStore(20);
        AgentService service = serviceWith(mockReplying(null), memory);

        ChatResult result = service.chat("hi", "conv-1");

        assertThat(result.reply()).isEmpty();
        assertThat(memory.history("conv-1")).extracting(m -> m.getText())
                .containsExactly("hi", "");
    }

    @Test
    void persistsTurnToMemory() {
        ChatMemoryStore memory = new InMemoryChatMemoryStore(20);
        AgentService service = serviceWith(mockReplying("reply"), memory);

        service.chat("question", "conv-1");

        assertThat(memory.history("conv-1")).extracting(m -> m.getText())
                .containsExactly("question", "reply");
    }
}
