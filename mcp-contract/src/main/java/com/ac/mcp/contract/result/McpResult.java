package com.ac.mcp.contract.result;

import com.ac.mcp.contract.presentation.PresentationHint;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

/**
 * MCP 数据工具的统一结果封装，携带数据、展示建议及业务错误。
 */
public record McpResult<T>(
        String specVersion,
        boolean success,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ResultSummary summary,
        PresentationHint presentation,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ResultMeta resultMeta,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        McpBusinessError error) {

    public static final String CURRENT_SPEC_VERSION = "1.1";
    public static final Set<String> SUPPORTED_SPEC_VERSIONS = Set.of("1.0", CURRENT_SPEC_VERSION);

    /**
     * 规范化契约版本和展示建议，并校验成功与失败结果的字段组合。
     */
    public McpResult {
        specVersion = specVersion == null || specVersion.isBlank() ? CURRENT_SPEC_VERSION : specVersion;
        if (!SUPPORTED_SPEC_VERSIONS.contains(specVersion)) {
            throw new IllegalArgumentException("Unsupported McpResult specVersion: " + specVersion);
        }
        presentation = presentation == null ? PresentationHint.none() : presentation;
        if (success && error != null) {
            throw new IllegalArgumentException("Successful McpResult must not contain error");
        }
        if (!success && data != null) {
            throw new IllegalArgumentException("Failed McpResult must contain null data");
        }
        if (!success && error == null) {
            throw new IllegalArgumentException("Failed McpResult requires error");
        }
    }

    /**
     * 判断结果是否使用当前契约版本。
     */
    @JsonIgnore
    public boolean isCurrentVersion() {
        return CURRENT_SPEC_VERSION.equals(specVersion);
    }

    /**
     * 创建当前版本的成功结果。
     */
    public static <T> McpResult<T> success(T data, ResultSummary summary,
                                           PresentationHint presentation, ResultMeta meta) {
        return new McpResult<>(CURRENT_SPEC_VERSION, true, data, summary, presentation, meta, null);
    }

    /**
     * 创建当前版本的业务失败结果。
     */
    public static <T> McpResult<T> failure(McpBusinessError error, ResultMeta meta) {
        return new McpResult<>(CURRENT_SPEC_VERSION, false, null, null, PresentationHint.none(), meta, error);
    }
}
