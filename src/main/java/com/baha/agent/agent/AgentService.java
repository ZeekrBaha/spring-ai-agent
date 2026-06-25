package com.baha.agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Drives one agent turn: replays conversation history, sends the user message
 * through the ChatClient (which runs the tool-call loop), records which tools
 * fired, and persists the turn to memory.
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ToolCallRecorder recorder;
    private final ChatMemoryStore memory;

    public AgentService(ChatClient chatClient, ToolCallRecorder recorder, ChatMemoryStore memory) {
        this.chatClient = chatClient;
        this.recorder = recorder;
        this.memory = memory;
    }

    public ChatResult chat(String message, String conversationId) {
        List<Message> history = memory.history(conversationId);

        recorder.start();
        String reply;
        try {
            reply = chatClient.prompt()
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
        List<String> toolsUsed = recorder.drain();

        memory.append(conversationId, new UserMessage(message), new AssistantMessage(reply));
        return new ChatResult(reply, toolsUsed);
    }
}
