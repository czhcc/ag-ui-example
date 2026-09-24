package com.ac.mcp.contract.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * 结果的请求标识、生成时间及扩展属性。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultMeta(String requestId, Instant generatedAt, Map<String, Object> attributes) {
    /**
     * 补齐生成时间，并保存扩展属性的不可变副本。
     */
    public ResultMeta {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
