package com.ac.mcp.contract.result;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpResultTest {
    @Test void defaultsSpecVersion() {
        var result = McpResult.success("ok", null, null, null);
        assertEquals("1.1", result.specVersion());
        assertTrue(result.success());
    }

    @Test void acceptsPreviousCompatibleVersion() {
        var result = new McpResult<>("1.0", true, "ok", null, null, null, null);
        assertFalse(result.isCurrentVersion());
    }

    @Test void rejectsInconsistentFailure() {
        assertThrows(IllegalArgumentException.class,
                () -> new McpResult<>("1.1", false, "unexpected", null, null, null,
                        new McpBusinessError("FAILED", "failed", false)));
    }
}
