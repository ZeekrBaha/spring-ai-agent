package com.baha.agent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Under the {@code postgres} profile with a real database, the @Primary
 * JdbcChatMemoryStore must win; Flyway must apply the schema and the app must
 * start cleanly. Proves profile wiring + datasource auto-config re-enable.
 */
@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers
class MemoryWiringPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    ChatMemoryStore memoryStore;

    @Test
    void postgresProfileSelectsJdbcStore() {
        assertThat(memoryStore).isInstanceOf(JdbcChatMemoryStore.class);
    }
}
