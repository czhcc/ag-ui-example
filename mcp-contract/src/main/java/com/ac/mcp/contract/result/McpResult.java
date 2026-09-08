package com.ac.mcp.contract.result;

import com.ac.mcp.contract.presentation.PresentationHint;

public record McpResult<T>(
        String specVersion,
        boolean success,
        T data,
        ResultSummary summary,
        PresentationHint presentation,
        ResultMeta resultMeta,
        McpBusinessError error) {

    public static final String CURRENT_SPEC_VERSION = "1.0";

    public McpResult {
        specVersion = specVersion == null || specVersion.isBlank() ? CURRENT_SPEC_VERSION : specVersion;
        presentation = presentation == null ? PresentationHint.none() : presentation;
    }

    public static <T> McpResult<T> success(T data, ResultSummary summary,
                                           PresentationHint presentation, ResultMeta meta) {
        return new McpResult<>(CURRENT_SPEC_VERSION, true, data, summary, presentation, meta, null);
    }

    public static <T> McpResult<T> failure(McpBusinessError error, ResultMeta meta) {
        return new McpResult<>(CURRENT_SPEC_VERSION, false, null, null, PresentationHint.none(), meta, error);
    }
}
