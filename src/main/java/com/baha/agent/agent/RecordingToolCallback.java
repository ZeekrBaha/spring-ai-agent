package com.baha.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorates a ToolCallback so each invocation is recorded into a per-call
 * {@link ToolCallSink} (for the toolsUsed field) before delegating. The sink is
 * shared by reference, so capture is correct regardless of which thread runs the
 * call — including reactor threads during streaming. Transparent to Spring AI's
 * tool-call loop.
 */
public class RecordingToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(RecordingToolCallback.class);

    private final ToolCallback delegate;
    private final ToolCallSink sink;

    public RecordingToolCallback(ToolCallback delegate, ToolCallSink sink) {
        this.delegate = delegate;
        this.sink = sink;
    }

    /** Record + log the invocation by tool name only (args may contain sensitive input). */
    private void track() {
        String name = delegate.getToolDefinition().name();
        sink.record(name);
        log.info("tool call: {}", name);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        track();
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        track();
        return delegate.call(toolInput, toolContext);
    }
}
