package com.baha.agent.agent;

import com.baha.agent.tools.CalculatorTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class RecordingToolCallbackTest {

    @Test
    void recordsToolNameAndDelegatesResult() {
        ToolCallback raw = ToolCallbacks.from(new CalculatorTool())[0];
        ToolCallRecorder recorder = new ToolCallRecorder();
        ToolCallback wrapped = new RecordingToolCallback(raw, recorder);

        recorder.start();
        String result = wrapped.call("{\"expression\":\"2 + 2\"}");

        assertThat(result).isEqualTo("4");
        assertThat(recorder.drain()).containsExactly("calculate");
    }

    @Test
    void drainDedupesAndClears() {
        ToolCallRecorder recorder = new ToolCallRecorder();
        recorder.start();
        recorder.record("weather");
        recorder.record("weather");
        recorder.record("calculate");

        assertThat(recorder.drain()).containsExactly("weather", "calculate");
        assertThat(recorder.drain()).isEmpty();
    }
}
