package com.baha.agent.agent;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Conversation memory seam, keyed by conversationId. Implementations:
 * <ul>
 *   <li>{@link InMemoryChatMemoryStore} — bounded in-process (default).</li>
 *   <li>{@code JdbcChatMemoryStore} — Postgres-backed ({@code postgres} profile).</li>
 * </ul>
 * Each implementation keeps at most a bounded number of most-recent messages
 * per conversation.
 */
public interface ChatMemoryStore {

    /** Messages for a conversation, oldest first. Empty if unknown id. */
    List<Message> history(String conversationId);

    /** Append messages, evicting oldest beyond the per-conversation bound. */
    void append(String conversationId, Message... messages);
}
