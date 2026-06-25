package com.baha.agent.agent;

import com.baha.agent.tools.CalculatorTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class RecordingToolCallbackTest {

    @Test
    void recordsToolNameIntoSinkAndDelegatesResult() {
        ToolCallback raw = ToolCallbacks.from(new CalculatorTool())[0];
        ToolCallSink sink = new ToolCallSink();
        ToolCallback wrapped = new RecordingToolCallback(raw, sink);

        String result = wrapped.call("{\"expression\":\"2 + 2\"}");

        assertThat(result).isEqualTo("4");
        assertThat(sink.usedTools()).containsExactly("calculate");
    }

    @Test
    void delegatesToolDefinitionName() {
        ToolCallback raw = ToolCallbacks.from(new CalculatorTool())[0];
        ToolCallback wrapped = new RecordingToolCallback(raw, new ToolCallSink());

        assertThat(wrapped.getToolDefinition().name()).isEqualTo("calculate");
    }
}
