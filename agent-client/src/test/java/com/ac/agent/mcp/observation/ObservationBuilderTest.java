package com.ac.agent.mcp.observation;

import com.ac.mcp.contract.result.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ObservationBuilderTest {
    @Test void emitsReferenceAndSummaryWithoutRawData() {
        var result = McpResult.success(List.of("secret-raw-row"), new ResultSummary(1, 1, false, "one result", List.of()), null, null);
        var text = new DefaultObservationBuilder().build("tool", "result_1", result);
        assertTrue(text.contains("result_1"));
        assertFalse(text.contains("secret-raw-row"));
    }
}
