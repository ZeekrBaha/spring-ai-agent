package com.baha.agent.agent;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded in-memory conversation store, keyed by conversationId. Keeps at most
 * {@code maxMessages} most-recent messages per conversation (oldest dropped).
 * In-memory only — not persisted across restarts (MVP non-goal).
 */
@Component
public class ChatMemoryStore {

    private final int maxMessages;
    private final Map<String, Deque<Message>> store = new ConcurrentHashMap<>();

    public ChatMemoryStore() {
        this(20);
    }

    public ChatMemoryStore(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    /** Messages for a conversation, oldest first. Empty if unknown id. */
    public List<Message> history(String conversationId) {
        Deque<Message> q = store.get(conversationId);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    /** Append messages, evicting oldest beyond the bound. */
    public synchronized void append(String conversationId, Message... messages) {
        Deque<Message> q = store.computeIfAbsent(conversationId, k -> new ArrayDeque<>());
        for (Message m : messages) {
            q.addLast(m);
        }
        while (q.size() > maxMessages) {
            q.removeFirst();
        }
    }
}
