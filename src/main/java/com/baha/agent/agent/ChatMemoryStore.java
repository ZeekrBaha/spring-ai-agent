package com.baha.agent.agent;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded in-memory conversation store, keyed by conversationId.
 * <ul>
 *   <li>At most {@code maxMessages} most-recent messages per conversation.</li>
 *   <li>At most {@code maxConversations} conversations (LRU eviction), so a
 *       flood of new conversationIds cannot grow the map without bound.</li>
 * </ul>
 * In-memory only — not persisted across restarts (MVP non-goal). All access is
 * synchronized: the underlying LinkedHashMap is not thread-safe and
 * access-order mutates on read.
 */
@Component
public class ChatMemoryStore {

    private final int maxMessages;
    private final Map<String, Deque<Message>> store;

    public ChatMemoryStore() {
        this(20, 1000);
    }

    public ChatMemoryStore(int maxMessages) {
        this(maxMessages, 1000);
    }

    public ChatMemoryStore(int maxMessages, int maxConversations) {
        this.maxMessages = maxMessages;
        this.store = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Deque<Message>> eldest) {
                return size() > maxConversations;
            }
        };
    }

    /** Messages for a conversation, oldest first. Empty if unknown id. */
    public synchronized List<Message> history(String conversationId) {
        Deque<Message> q = store.get(conversationId);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    /** Append messages, evicting oldest beyond the per-conversation bound. */
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
