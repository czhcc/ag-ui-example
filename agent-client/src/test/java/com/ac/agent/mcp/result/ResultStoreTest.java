package com.ac.agent.mcp.result;

import com.ac.mcp.contract.result.McpResult;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class ResultStoreTest {
    @Test void roundTripsResult() {
        var store = new InMemoryResultStore(Duration.ofMinutes(30), new ResultRefGenerator());
        var ref = store.save("knowledge", "tool", McpResult.success("data", null, null, null));
        assertEquals("data", store.get(ref).result().data());
    }
}
