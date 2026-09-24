package com.ac.mcp.contract.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 工具返回的业务失败信息，与协议或传输错误区分。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpBusinessError(
        String code,
        String message,
        boolean retryable,
        Map<String, Object> details) {

    /**
     * 保存错误详情的不可变副本。
     */
    public McpBusinessError {
        details = details == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    /**
     * 创建不含额外详情的业务错误。
     */
    public McpBusinessError(String code, String message, boolean retryable) {
        this(code, message, retryable, Map.of());
    }
}
