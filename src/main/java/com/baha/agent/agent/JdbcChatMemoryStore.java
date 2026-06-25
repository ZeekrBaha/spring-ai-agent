package com.baha.agent.agent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Postgres-backed {@link ChatMemoryStore}. Each message is one row in
 * {@code chat_message}; {@code (conversation_id, seq)} orders messages within a
 * conversation. Keeps at most {@code maxMessages} most-recent rows per
 * conversation (older rows pruned on append). Survives restarts.
 *
 * <p>Active only under the {@code postgres} profile, where it is {@code @Primary}
 * so it wins over the default {@link InMemoryChatMemoryStore}. All SQL is
 * parameterized (no string-built queries).
 */
@Component
@Primary
@Profile("postgres")
public class JdbcChatMemoryStore implements ChatMemoryStore {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final JdbcTemplate jdbc;
    private final int maxMessages;

    @Autowired
    public JdbcChatMemoryStore(JdbcTemplate jdbc,
                               @Value("${agent.memory.max-messages:20}") int maxMessages) {
        this.jdbc = jdbc;
        this.maxMessages = maxMessages;
    }

    @Override
    public List<Message> history(String conversationId) {
        // Most-recent maxMessages, then reverse to oldest-first.
        List<Message> recentFirst = jdbc.query(
                "SELECT role, content FROM chat_message WHERE conversation_id = ? "
                        + "ORDER BY seq DESC LIMIT ?",
                (rs, n) -> toMessage(rs.getString("role"), rs.getString("content")),
                conversationId, maxMessages);
        List<Message> oldestFirst = new ArrayList<>(recentFirst);
        Collections.reverse(oldestFirst);
        return oldestFirst;
    }

    @Override
    public synchronized void append(String conversationId, Message... messages) {
        Long maxSeq = jdbc.queryForObject(
                "SELECT COALESCE(MAX(seq), -1) FROM chat_message WHERE conversation_id = ?",
                Long.class, conversationId);
        long seq = (maxSeq == null ? -1 : maxSeq) + 1;

        for (Message m : messages) {
            jdbc.update(
                    "INSERT INTO chat_message (conversation_id, seq, role, content) VALUES (?, ?, ?, ?)",
                    conversationId, seq++, role(m), m.getText());
        }

        // Prune anything older than the most-recent maxMessages for this conversation.
        jdbc.update(
                "DELETE FROM chat_message WHERE conversation_id = ? AND seq <= ?",
                conversationId, seq - 1 - maxMessages);
    }

    private String role(Message m) {
        return m instanceof AssistantMessage ? ROLE_ASSISTANT : ROLE_USER;
    }

    private Message toMessage(String role, String content) {
        return ROLE_ASSISTANT.equals(role) ? new AssistantMessage(content) : new UserMessage(content);
    }
}
