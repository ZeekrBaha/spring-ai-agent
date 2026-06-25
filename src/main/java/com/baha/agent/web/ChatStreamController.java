package com.baha.agent.web;

import com.baha.agent.agent.AgentService;
import com.baha.agent.agent.ChatStreamEvent;
import com.baha.agent.web.dto.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Server-Sent Events streaming endpoint. Emits {@code token} events as the
 * reply is generated, then a terminal {@code done} (or {@code error}) event.
 * Each SSE event is named by {@link ChatStreamEvent#type()}.
 */
@RestController
@RequestMapping("/api/chat/stream")
public class ChatStreamController {

    private final AgentService agentService;

    public ChatStreamController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEvent>> stream(@Valid @RequestBody ChatRequest request) {
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        return agentService.chatStream(request.message(), conversationId)
                .map(event -> ServerSentEvent.<ChatStreamEvent>builder(event)
                        .event(event.type())
                        .build());
    }
}
