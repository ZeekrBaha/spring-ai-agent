package com.baha.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorates a ToolCallback so each invocation is recorded (for the toolsUsed
 * field) before delegating. Transparent to Spring AI's tool-call loop.
 */
public class RecordingToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(RecordingToolCallback.class);

    private final ToolCallback delegate;
    private final ToolCallRecorder recorder;

    public RecordingToolCallback(ToolCallback delegate, ToolCallRecorder recorder) {
        this.delegate = delegate;
        this.recorder = recorder;
    }

    /** Record + log the invocation by tool name only (args may contain sensitive input). */
    private void track() {
        String name = delegate.getToolDefinition().name();
        recorder.record(name);
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
