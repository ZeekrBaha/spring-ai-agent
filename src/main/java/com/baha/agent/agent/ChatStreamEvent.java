package com.baha.agent.agent;

import java.util.List;

/**
 * One event in a streamed agent turn. {@code type} is one of:
 * <ul>
 *   <li>{@code "token"} — a chunk of the reply ({@code text} set).</li>
 *   <li>{@code "done"} — terminal success ({@code reply}, {@code toolsUsed},
 *       {@code conversationId} set).</li>
 *   <li>{@code "error"} — terminal failure ({@code error} set).</li>
 * </ul>
 * Unused fields are null/empty for the given type.
 */
public record ChatStreamEvent(
        String type,
        String text,
        String reply,
        List<String> toolsUsed,
        String conversationId,
        String error) {

    public static ChatStreamEvent token(String text) {
        return new ChatStreamEvent("token", text, null, null, null, null);
    }

    public static ChatStreamEvent done(String reply, List<String> toolsUsed, String conversationId) {
        return new ChatStreamEvent("done", null, reply, toolsUsed, conversationId, null);
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", null, null, null, null, message);
    }
}
