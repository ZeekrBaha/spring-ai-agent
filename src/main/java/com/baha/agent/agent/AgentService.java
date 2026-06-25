package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Drives one agent turn: replays conversation history, sends the user message
 * through the ChatClient (which runs the tool-call loop), records which tools
 * fired via a fresh per-call {@link ToolCallSink}, and persists the turn.
 *
 * <p>The sink is created per call and passed by reference into that call's
 * recording tool callbacks, so capture is correct even when the tool loop runs
 * on other threads (streaming) — unlike a ThreadLocal.
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final AgentTools agentTools;
    private final ChatMemoryStore memory;
    private final MeterRegistry meterRegistry;
    private final Counter chatRequests;

    public AgentService(ChatClient chatClient, AgentTools agentTools, ChatMemoryStore memory,
                        MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.agentTools = agentTools;
        this.memory = memory;
        this.meterRegistry = meterRegistry;
        this.chatRequests = Counter.builder("agent.chat.requests").register(meterRegistry);
    }

    public ChatResult chat(String message, String conversationId) {
        chatRequests.increment();
        List<Message> history = memory.history(conversationId);
        ToolCallSink sink = new ToolCallSink();

        String reply;
        try {
            reply = chatClient.prompt()
                    .toolCallbacks(recordingCallbacks(sink))
                    .messages(history)
                    .user(message)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            // Model/tool-loop failure (network, auth, rate limit) — not our bug.
            throw new AgentUpstreamException(e);
        }
        if (reply == null) {
            reply = ""; // model can return no content; never propagate null into memory
        }
        List<String> toolsUsed = sink.usedTools();

        memory.append(conversationId, new UserMessage(message), new AssistantMessage(reply));
        return new ChatResult(reply, toolsUsed);
    }

    /**
     * Streaming variant: emits {@code token} events as the reply is generated,
     * then a terminal {@code done} event carrying the full reply, captured
     * toolsUsed, and conversationId. Persists the turn on completion. Any
     * upstream failure terminates as a single {@code error} event (no hang).
     */
    public Flux<ChatStreamEvent> chatStream(String message, String conversationId) {
        chatRequests.increment();
        List<Message> history = memory.history(conversationId);
        ToolCallSink sink = new ToolCallSink();
        StringBuilder full = new StringBuilder();

        Flux<ChatStreamEvent> tokens = chatClient.prompt()
                .toolCallbacks(recordingCallbacks(sink))
                .messages(history)
                .user(message)
                .stream()
                .content()
                .doOnNext(full::append)
                .map(ChatStreamEvent::token);

        Mono<ChatStreamEvent> done = Mono.fromSupplier(() -> {
            String reply = full.toString();
            memory.append(conversationId, new UserMessage(message), new AssistantMessage(reply));
            return ChatStreamEvent.done(reply, sink.usedTools(), conversationId);
        });

        return tokens.concatWith(done)
                .onErrorResume(e -> Flux.just(ChatStreamEvent.error(
                        "The agent could not complete your request (upstream error).")));
    }

    /** Wrap the raw tool callbacks for this call so invocations land in {@code sink}. */
    List<ToolCallback> recordingCallbacks(ToolCallSink sink) {
        return agentTools.callbacks().stream()
                .map(cb -> (ToolCallback) new RecordingToolCallback(cb, sink, meterRegistry))
                .toList();
    }
}
