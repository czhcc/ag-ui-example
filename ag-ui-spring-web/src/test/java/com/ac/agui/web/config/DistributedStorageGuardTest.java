package com.ac.agui.web.config;

import com.ac.agui.web.stream.AgUiEventStream;
import com.ac.richui.core.result.InMemoryResultStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证多副本模式对共享存储的要求。 */
class DistributedStorageGuardTest {
    @Test
    void rejectsProcessLocalStoresOnlyInDistributedMode() {
        var results = new InMemoryResultStore(Duration.ofMinutes(5), 10);
        var runs = new AgUiEventStream(new ObjectMapper(), Duration.ofMinutes(5), 32, 128);

        assertDoesNotThrow(() -> new DistributedStorageGuard(false, results, runs));
        assertThrows(IllegalStateException.class,
                () -> new DistributedStorageGuard(true, results, runs));
    }
}
