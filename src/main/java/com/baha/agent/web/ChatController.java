package com.baha.agent.web;

import com.baha.agent.agent.AgentService;
import com.baha.agent.agent.ChatResult;
import com.baha.agent.web.dto.ChatRequest;
import com.baha.agent.web.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        ChatResult result = agentService.chat(request.message(), conversationId);
        return new ChatResponse(result.reply(), result.toolsUsed(), conversationId);
    }
}
