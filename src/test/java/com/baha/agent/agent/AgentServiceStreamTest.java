package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentServiceStreamTest {

    private final AgentTools noTools = new AgentTools(List.of());

    private ChatClient mockStreaming(Flux<String> tokens) {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().toolCallbacks(anyList()).messages(anyList())
                .user(anyString()).stream().content())
                .thenReturn(tokens);
        return client;
    }

    @Test
    void emitsTokensThenTerminalDoneWithFullReply() {
        ChatMemoryStore memory = new InMemoryChatMemoryStore(20);
        AgentService service = new AgentService(mockStreaming(Flux.just("Hel", "lo")), noTools, memory, new SimpleMeterRegistry());

        StepVerifier.create(service.chatStream("hi", "c1"))
                .assertNext(e -> { assertThat(e.type()).isEqualTo("token"); assertThat(e.text()).isEqualTo("Hel"); })
                .assertNext(e -> { assertThat(e.type()).isEqualTo("token"); assertThat(e.text()).isEqualTo("lo"); })
                .assertNext(e -> {
                    assertThat(e.type()).isEqualTo("done");
                    assertThat(e.reply()).isEqualTo("Hello");
                    assertThat(e.conversationId()).isEqualTo("c1");
                    assertThat(e.toolsUsed()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void persistsFullReplyToMemoryOnCompletion() {
        ChatMemoryStore memory = new InMemoryChatMemoryStore(20);
        AgentService service = new AgentService(mockStreaming(Flux.just("a", "b")), noTools, memory, new SimpleMeterRegistry());

        service.chatStream("q", "c1").blockLast();

        assertThat(memory.history("c1")).extracting(m -> m.getText())
                .containsExactly("q", "ab");
    }

    @Test
    void streamErrorEmitsTerminalErrorEvent() {
        ChatMemoryStore memory = new InMemoryChatMemoryStore(20);
        AgentService service = new AgentService(
                mockStreaming(Flux.error(new RuntimeException("upstream boom"))), noTools, memory, new SimpleMeterRegistry());

        StepVerifier.create(service.chatStream("hi", "c1"))
                .assertNext(e -> {
                    assertThat(e.type()).isEqualTo("error");
                    assertThat(e.error()).isNotBlank();
                })
                .verifyComplete();
    }
}
