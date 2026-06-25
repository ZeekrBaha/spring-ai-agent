package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import com.baha.agent.tools.CalculatorTool;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
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
    void perCallStreamingCallbacksCaptureToolFiredOnAnotherThread() throws InterruptedException {
        // During streaming the tool loop runs off the request thread. The
        // callbacks AgentService builds for a turn must still land in that
        // turn's sink — this would FAIL with a ThreadLocal recorder.
        ToolCallback calculator = ToolCallbacks.from(new CalculatorTool())[0];
        AgentService service = new AgentService(mockStreaming(Flux.just("x")),
                new AgentTools(List.of(calculator)), new InMemoryChatMemoryStore(20),
                new SimpleMeterRegistry());

        ToolCallSink sink = new ToolCallSink();
        List<ToolCallback> perCall = service.recordingCallbacks(sink);

        Thread worker = new Thread(() -> perCall.get(0).call("{\"expression\":\"2 + 2\"}"));
        worker.start();
        worker.join();

        assertThat(sink.usedTools()).containsExactly("calculate");
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
