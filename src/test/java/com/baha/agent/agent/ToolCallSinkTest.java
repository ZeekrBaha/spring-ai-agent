package com.baha.agent.agent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallSinkTest {

    @Test
    void capturesNamesRecordedFromAnotherThread() throws InterruptedException {
        // Streaming runs the tool loop on reactor threads, not the caller's.
        // A ThreadLocal-based recorder would miss these; a shared sink must not.
        ToolCallSink sink = new ToolCallSink();
        CountDownLatch done = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            sink.record("weather");
            done.countDown();
        });
        worker.start();
        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(sink.usedTools()).containsExactly("weather");
    }

    @Test
    void returnsDistinctNamesInFirstSeenOrder() {
        ToolCallSink sink = new ToolCallSink();
        sink.record("weather");
        sink.record("weather");
        sink.record("calculate");

        assertThat(sink.usedTools()).containsExactly("weather", "calculate");
    }

    @Test
    void emptyWhenNothingRecorded() {
        assertThat(new ToolCallSink().usedTools()).isEmpty();
    }
}
