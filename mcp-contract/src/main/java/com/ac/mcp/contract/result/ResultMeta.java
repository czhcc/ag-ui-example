package com.ac.mcp.contract.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultMeta(String requestId, Instant generatedAt, Map<String, Object> attributes) {
    public ResultMeta {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
