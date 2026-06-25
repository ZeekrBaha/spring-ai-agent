package com.baha.agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Drives one agent turn: sends the user message through the ChatClient (which
 * runs the tool-call loop) and reports the reply plus which tools fired.
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ToolCallRecorder recorder;

    public AgentService(ChatClient chatClient, ToolCallRecorder recorder) {
        this.chatClient = chatClient;
        this.recorder = recorder;
    }

    public ChatResult chat(String message) {
        recorder.start();
        String reply = chatClient.prompt().user(message).call().content();
        List<String> toolsUsed = recorder.drain();
        return new ChatResult(reply, toolsUsed);
    }
}
