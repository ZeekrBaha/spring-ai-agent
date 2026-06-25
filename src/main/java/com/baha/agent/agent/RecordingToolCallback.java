package com.baha.agent.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorates a ToolCallback so each invocation is recorded (for the toolsUsed
 * field) before delegating. Transparent to Spring AI's tool-call loop.
 */
public class RecordingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolCallRecorder recorder;

    public RecordingToolCallback(ToolCallback delegate, ToolCallRecorder recorder) {
        this.delegate = delegate;
        this.recorder = recorder;
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
        recorder.record(delegate.getToolDefinition().name());
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        recorder.record(delegate.getToolDefinition().name());
        return delegate.call(toolInput, toolContext);
    }
}
