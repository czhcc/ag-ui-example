package com.ac.mcp.contract.result;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpResultTest {
    @Test void defaultsSpecVersion() {
        var result = McpResult.success("ok", null, null, null);
        assertEquals("1.0", result.specVersion());
        assertTrue(result.success());
    }
}
