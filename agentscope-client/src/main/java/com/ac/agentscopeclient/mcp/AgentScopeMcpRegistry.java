package com.ac.agentscopeclient.mcp;

import com.ac.agentscopeclient.config.AgentScopeMcpProperties;
import com.ac.runtime.agentscope.AgentScopeMcpTool;
import com.ac.richui.core.tool.*;
import com.ac.richui.mcp.McpCallToolResultAdapter;
import com.ac.mcpapps.core.*;
import com.ac.mcpapps.web.McpAppResultPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.AgentTool;
import io.modelcontextprotocol.client.*;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import java.util.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Native MCP resources supplement AgentScope; Agent execution still uses AgentScope. */
@Component
public final class AgentScopeMcpRegistry {
    private final Map<String, McpSyncClient> clients = new LinkedHashMap<>();
    private final List<AgentTool> tools;
    public AgentScopeMcpRegistry(AgentScopeMcpProperties properties, McpCallToolResultAdapter results,
            RichToolResultProcessor processor, ObjectMapper mapper, McpAppCatalog apps, McpAppResultPublisher publisher) {
        List<AgentTool> discovered = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (var server : properties.servers()) {
            if (!server.enabled()) continue;
            var builder = HttpClientStreamableHttpTransport.builder(server.baseUrl()).endpoint(server.endpoint());
            switch (server.auth().type()) {
                case BEARER -> builder.httpRequestCustomizer((r, method, endpoint, body, context) -> r.header("Authorization", "Bearer " + server.auth().token()));
                case API_KEY, CUSTOM_HEADER -> builder.httpRequestCustomizer((r, method, endpoint, body, context) -> r.header(server.auth().headerName(), server.auth().headerValue()));
                case NONE -> { }
            }
            builder.httpRequestCustomizer((request, method, endpoint, body, context) -> {
                com.ac.mcpapps.web.McpAppsInitialization.customize(request, method, body);
                switch (server.auth().type()) {
                    case BEARER -> request.header("Authorization", "Bearer " + server.auth().token());
                    case API_KEY, CUSTOM_HEADER -> request.header(server.auth().headerName(), server.auth().headerValue());
                    case NONE -> { }
                }
            });
            var client = McpClient.sync(builder.build()).requestTimeout(server.timeout()).build();
            client.initialize(); clients.put(server.code(), client);
            apps.transport(server.code(), new McpAppTransport() {
                public Map<String, Object> readResource(String uri) {
                    return mapper.convertValue(client.readResource(new McpSchema.ReadResourceRequest(uri)), Map.class);
                }
                public Map<String, Object> callTool(String name, Map<String, Object> args, com.ac.richui.core.context.AccessSubject subject) {
                    return mapper.convertValue(client.callTool(McpSchema.CallToolRequest.builder().name(name).arguments(args)
                            .meta(Map.of("ac.subject", Map.of("tenantId", subject.tenantId(), "userId", subject.userId()))).build()), Map.class);
                }
            });
            for (var tool : client.listTools().tools()) {
                Map<String, Object> schema = mapper.convertValue(tool.inputSchema(), Map.class);
                var appTool = apps.register(server.code(), tool.name(), tool.title(), schema, tool.meta(),
                        tool.annotations() != null && Boolean.TRUE.equals(tool.annotations().readOnlyHint()));
                if (!appTool.modelVisible()) continue;
                String name = server.toolPrefix() + tool.name();
                if (!names.add(name)) throw new IllegalStateException("MCP tool name collision: " + name);
                discovered.add(new AgentScopeMcpTool(name, tool.description(), schema, server.code(), tool.name(),
                        (remote, args, scope) -> Mono.fromCallable(() -> client.callTool(McpSchema.CallToolRequest.builder()
                                .name(remote).arguments(args).meta(Map.of("ac.subject", Map.of(
                                        "tenantId", scope.tenantId(), "userId", scope.userId()))).build()))
                                .subscribeOn(Schedulers.boundedElastic()), results, processor,
                        (scope, processed, raw) -> publisher.publish(scope, new ToolIdentity(server.code(), tool.name()), processed, raw)));
            }
        }
        tools = List.copyOf(discovered);
    }
    public List<AgentTool> tools() { return tools; }
    @PreDestroy public void close() { clients.values().forEach(McpSyncClient::close); clients.clear(); }
}
