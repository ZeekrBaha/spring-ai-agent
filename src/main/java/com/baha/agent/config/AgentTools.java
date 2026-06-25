package com.baha.agent.config;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Holder for the agent's registered tool callbacks. A distinct type (not a
 * raw List&lt;ToolCallback&gt;) so it can be injected unambiguously and asserted
 * by the startup registration test.
 */
public record AgentTools(List<ToolCallback> callbacks) {
}
