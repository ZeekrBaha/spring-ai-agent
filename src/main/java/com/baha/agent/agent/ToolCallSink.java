package com.baha.agent.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Per-call accumulator of invoked tool names. Created fresh for each agent turn
 * and passed by reference into that turn's {@link RecordingToolCallback}s, so
 * capture is call-scoped and thread-safe — correct even when the tool loop runs
 * on reactor threads during streaming (unlike a ThreadLocal, which would only
 * see records made on the caller's own thread).
 */
public class ToolCallSink {

    private final List<String> names = new ArrayList<>();

    /** Record one tool invocation by name. Thread-safe. */
    public synchronized void record(String toolName) {
        names.add(toolName);
    }

    /** Distinct tool names in first-seen order. Thread-safe snapshot. */
    public synchronized List<String> usedTools() {
        return new ArrayList<>(new LinkedHashSet<>(names));
    }
}
