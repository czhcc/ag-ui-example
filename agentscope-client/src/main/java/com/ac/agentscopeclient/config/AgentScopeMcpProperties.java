package com.ac.agentscopeclient.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ac.agentscope.mcp")
public record AgentScopeMcpProperties(List<Server> servers) {
    public AgentScopeMcpProperties {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public record Server(
            String code, String name, String baseUrl, String endpoint, Duration timeout,
            boolean enabled, String toolPrefix, Auth auth) {
        public Server {
            if (code == null || code.isBlank()) throw new IllegalArgumentException("MCP code must not be blank");
            if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("MCP baseUrl must not be blank");
            endpoint = endpoint == null || endpoint.isBlank() ? "/mcp" : endpoint;
            timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
            toolPrefix = toolPrefix == null ? "" : toolPrefix;
            auth = auth == null ? new Auth(null, null, null, null) : auth;
        }

        public String url() {
            return baseUrl.replaceAll("/+$", "") + "/" + endpoint.replaceAll("^/+", "");
        }
    }

    public record Auth(Type type, String token, String headerName, String headerValue) {
        public Auth { type = type == null ? Type.NONE : type; }
        public enum Type { NONE, BEARER, API_KEY, CUSTOM_HEADER }
    }
}
