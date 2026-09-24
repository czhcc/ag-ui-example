package com.ac.richui.mcp;

import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.result.McpResult;
import com.ac.mcp.contract.result.ResultSummary;
import com.ac.richui.core.tool.RawToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

/**
 * 按固定优先级解码 MCP 结果：先处理 isError，再取结构化内容，最后降级解析文本。
 */
public final class DefaultMcpResultDecoder implements McpResultDecoder {
    private final ObjectMapper objectMapper;

    /**
     * 使用指定 JSON 映射器创建解码器。
     */
    public DefaultMcpResultDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * 将原始工具结果识别为 Rich Result、业务错误、MCP 错误或普通结果。
     */
    @Override
    public DecodedMcpResult decode(RawToolResult raw) {
        Objects.requireNonNull(raw, "raw must not be null");
        if (raw.error()) {
            String message = raw.hasTextContent() ? raw.textContent() : "MCP tool execution failed";
            return new DecodedMcpResult(DecodedMcpResult.Kind.MCP_ERROR, null, message);
        }
        if (raw.hasStructuredContent()) {
            return decodeObject(raw.structuredContent());
        }
        if (!raw.hasTextContent()) {
            return new DecodedMcpResult(DecodedMcpResult.Kind.PLAIN_RESULT, null, "");
        }
        try {
            JsonNode json = objectMapper.readTree(raw.textContent());
            if (json != null && json.isObject()) {
                Map<String, Object> value = objectMapper.convertValue(json, new TypeReference<>() {
                });
                return decodeObject(value);
            }
        } catch (JsonProcessingException ignored) {
            // 非 JSON 文本也是有效的普通 MCP 工具结果。
        }
        return new DecodedMcpResult(DecodedMcpResult.Kind.PLAIN_RESULT, null, raw.textContent());
    }

    private DecodedMcpResult decodeObject(Map<String, Object> value) {
        if (!looksLikeRichEnvelope(value)) {
            McpResult<?> generic = McpResult.success(
                    value,
                    new ResultSummary(null, null, null, "工具返回结构化结果", null),
                    PresentationHint.none(),
                    null);
            return new DecodedMcpResult(DecodedMcpResult.Kind.RICH_RESULT, generic, "工具返回结构化结果");
        }
        try {
            McpResult<?> result = objectMapper.convertValue(value, new TypeReference<McpResult<Object>>() {
            });
            return new DecodedMcpResult(
                    result.success() ? DecodedMcpResult.Kind.RICH_RESULT : DecodedMcpResult.Kind.BUSINESS_ERROR,
                    result,
                    "");
        } catch (IllegalArgumentException exception) {
            return new DecodedMcpResult(DecodedMcpResult.Kind.INVALID_RICH_RESULT, null,
                    "MCP rich result did not satisfy the contract");
        }
    }

    private boolean looksLikeRichEnvelope(Map<String, Object> value) {
        return value.containsKey("success")
                || value.containsKey("specVersion")
                || value.containsKey("presentation")
                || value.containsKey("summary")
                || value.containsKey("error");
    }
}
