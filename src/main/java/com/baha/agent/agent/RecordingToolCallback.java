package com.baha.agent.agent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;

    public RecordingToolCallback(ToolCallback delegate, ToolCallSink sink, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.sink = sink;
        this.meterRegistry = meterRegistry;
    }

    /** Record + log + meter the invocation by tool name only (args may be sensitive). */
    private void track() {
        String name = delegate.getToolDefinition().name();
        sink.record(name);
        Counter.builder("agent.tool.invocations").tag("tool", name)
                .register(meterRegistry).increment();
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
