package com.baha.agent.agent;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class JdbcChatMemoryStoreIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static DataSource dataSource;
    JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        dataSource = ds;
        Flyway.configure().dataSource(ds).load().migrate();
    }

    @BeforeEach
    void cleanTable() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("TRUNCATE chat_message");
    }

    @Test
    void appendThenHistoryReturnsMessagesInOrder() {
        JdbcChatMemoryStore store = new JdbcChatMemoryStore(jdbc, 20);
        store.append("c1", new UserMessage("hi"), new AssistantMessage("hello"));

        assertThat(store.history("c1")).extracting(Message::getText)
                .containsExactly("hi", "hello");
    }

    @Test
    void boundedToMaxMessagesDroppingOldest() {
        JdbcChatMemoryStore store = new JdbcChatMemoryStore(jdbc, 4);
        store.append("c1", new UserMessage("u1"), new AssistantMessage("a1"));
        store.append("c1", new UserMessage("u2"), new AssistantMessage("a2"));
        store.append("c1", new UserMessage("u3"), new AssistantMessage("a3"));

        assertThat(store.history("c1")).extracting(Message::getText)
                .containsExactly("u2", "a2", "u3", "a3");
    }

    @Test
    void historyIsolatedPerConversation() {
        JdbcChatMemoryStore store = new JdbcChatMemoryStore(jdbc, 20);
        store.append("c1", new UserMessage("u1"));

        assertThat(store.history("c2")).isEmpty();
    }

    @Test
    void rolesRoundTripUserVsAssistant() {
        JdbcChatMemoryStore store = new JdbcChatMemoryStore(jdbc, 20);
        store.append("c1", new UserMessage("q"), new AssistantMessage("a"));

        var history = store.history("c1");
        assertThat(history.get(0)).isInstanceOf(UserMessage.class);
        assertThat(history.get(1)).isInstanceOf(AssistantMessage.class);
    }

    @Test
    void persistenceSurvivesNewStoreInstance() {
        new JdbcChatMemoryStore(jdbc, 20)
                .append("c1", new UserMessage("durable"));

        // A fresh store instance over the same database still sees the history.
        JdbcChatMemoryStore reopened = new JdbcChatMemoryStore(new JdbcTemplate(dataSource), 20);
        assertThat(reopened.history("c1")).extracting(Message::getText)
                .containsExactly("durable");
    }
}
