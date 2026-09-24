package com.ac.agent.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;

class SaaDistributedStateGuardTest {
    @Test
    void distributedModeRejectsMemoryCheckpointSaver() {
        assertDoesNotThrow(() -> new SaaDistributedStateGuard(false, new MemorySaver()));
        assertThrows(IllegalStateException.class,
                () -> new SaaDistributedStateGuard(true, new MemorySaver()));
    }
}
