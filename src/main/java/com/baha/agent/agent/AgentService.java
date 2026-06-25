package com.baha.agent.agent;

import com.baha.agent.config.AgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

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

    public AgentService(ChatClient chatClient, AgentTools agentTools, ChatMemoryStore memory) {
        this.chatClient = chatClient;
        this.agentTools = agentTools;
        this.memory = memory;
    }

    public ChatResult chat(String message, String conversationId) {
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

    /** Wrap the raw tool callbacks for this call so invocations land in {@code sink}. */
    List<ToolCallback> recordingCallbacks(ToolCallSink sink) {
        return agentTools.callbacks().stream()
                .map(cb -> (ToolCallback) new RecordingToolCallback(cb, sink))
                .toList();
    }
}
