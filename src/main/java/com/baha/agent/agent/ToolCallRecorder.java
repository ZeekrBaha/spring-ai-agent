package com.baha.agent.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Records which tools were invoked during a single synchronous agent call.
 * The tool-call loop runs on the calling thread, so a ThreadLocal cleanly
 * scopes the captured names to one request without cross-talk.
 */
@Component
public class ToolCallRecorder {

    private final ThreadLocal<List<String>> calls = ThreadLocal.withInitial(ArrayList::new);

    /** Begin a fresh capture for the current thread/request. */
    public void start() {
        calls.get().clear();
    }

    /** Record one tool invocation by name. */
    public void record(String toolName) {
        calls.get().add(toolName);
    }

    /** Return distinct tool names (in first-seen order) and clear the buffer. */
    public List<String> drain() {
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(calls.get()));
        calls.get().clear();
        return distinct;
    }
}
