package com.baha.agent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryStoreTest {

    @Test
    void appendThenHistoryReturnsMessagesInOrder() {
        ChatMemoryStore store = new InMemoryChatMemoryStore(20);
        store.append("c1", new UserMessage("hi"), new AssistantMessage("hello"));

        var history = store.history("c1");
        assertThat(history).extracting(Message::getText).containsExactly("hi", "hello");
    }

    @Test
    void boundedToMaxMessagesDroppingOldest() {
        ChatMemoryStore store = new InMemoryChatMemoryStore(4);
        store.append("c1", new UserMessage("u1"), new AssistantMessage("a1"));
        store.append("c1", new UserMessage("u2"), new AssistantMessage("a2"));
        store.append("c1", new UserMessage("u3"), new AssistantMessage("a3"));

        var history = store.history("c1");
        assertThat(history).hasSize(4);
        assertThat(history).extracting(Message::getText)
                .containsExactly("u2", "a2", "u3", "a3");
    }

    @Test
    void boundedConversationCountEvictsOldest() {
        ChatMemoryStore store = new InMemoryChatMemoryStore(20, 2);
        store.append("c1", new UserMessage("u1"));
        store.append("c2", new UserMessage("u2"));
        store.append("c3", new UserMessage("u3")); // evicts c1 (oldest)

        assertThat(store.history("c1")).isEmpty();
        assertThat(store.history("c2")).extracting(Message::getText).containsExactly("u2");
        assertThat(store.history("c3")).extracting(Message::getText).containsExactly("u3");
    }

    @Test
    void historyIsolatedPerConversation() {
        ChatMemoryStore store = new InMemoryChatMemoryStore(20);
        store.append("c1", new UserMessage("u1"), new AssistantMessage("a1"));

        assertThat(store.history("c2")).isEmpty();
    }
}
