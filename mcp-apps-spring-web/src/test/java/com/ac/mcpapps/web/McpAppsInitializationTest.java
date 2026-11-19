package com.ac.mcpapps.web;

import java.net.URI;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpAppsInitializationTest {
    @Test void addsNegotiationWithoutChangingOtherRequests() {
        var builder = HttpRequest.newBuilder(URI.create("https://example.test/mcp")).header("Authorization", "Bearer test");
        McpAppsInitialization.customize(builder, "POST", "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"capabilities\":{}}}");
        assertTrue(builder.build().bodyPublisher().orElseThrow().contentLength() > 100);
        assertEquals("Bearer test", builder.build().headers().firstValue("Authorization").orElseThrow());
        var ping = HttpRequest.newBuilder(URI.create("https://example.test/mcp"));
        McpAppsInitialization.customize(ping, "POST", "{\"method\":\"ping\"}");
        assertTrue(ping.build().bodyPublisher().isEmpty());
    }
}
