package com.baha.agent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With no active profile (no database), the injected ChatMemoryStore must be
 * the in-memory implementation, and the app must boot without a datasource.
 */
@SpringBootTest
class MemoryWiringTest {

    @Autowired
    ChatMemoryStore memoryStore;

    @Test
    void defaultStoreIsInMemory() {
        assertThat(memoryStore).isInstanceOf(InMemoryChatMemoryStore.class);
    }
}
