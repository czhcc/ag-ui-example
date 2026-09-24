package com.ac.mcp.contract.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpBusinessError(
        String code,
        String message,
        boolean retryable,
        Map<String, Object> details) {

    public McpBusinessError {
        details = details == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public McpBusinessError(String code, String message, boolean retryable) {
        this(code, message, retryable, Map.of());
    }
}
