package com.ac.agent.mcp.client;

import com.ac.agent.mcp.config.McpServerDefinition;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class McpClientFactory {
    private static final Logger log = LoggerFactory.getLogger(McpClientFactory.class);

    public McpSyncClient create(McpServerDefinition definition) {
        var builder = HttpClientStreamableHttpTransport.builder(definition.baseUrl()).endpoint(definition.endpoint());
        switch (definition.auth().type()) {
            case BEARER -> builder.httpRequestCustomizer((request, method, endpoint, body, context) ->
                    request.header("Authorization", "Bearer " + definition.auth().token()));
            case API_KEY, CUSTOM_HEADER -> builder.httpRequestCustomizer((request, method, endpoint, body, context) ->
                    request.header(definition.auth().headerName(), definition.auth().headerValue()));
            case NONE -> { }
        }
        var client = McpClient.sync(builder.build()).requestTimeout(definition.timeout()).build();
        log.info("Initializing MCP connection serverCode={} endpoint={}", definition.code(), definition.endpoint());
        client.initialize();
        return client;
    }
}
