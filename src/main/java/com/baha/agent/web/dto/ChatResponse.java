package com.baha.agent.web.dto;

import java.util.List;

/** Outbound chat response. */
public record ChatResponse(String reply, List<String> toolsUsed, String conversationId) {
}
