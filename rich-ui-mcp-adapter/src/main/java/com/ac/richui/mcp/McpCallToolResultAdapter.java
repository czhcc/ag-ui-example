package com.ac.richui.mcp;

import com.ac.richui.core.tool.RawToolResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 将 MCP SDK 的工具调用结果转换为与 Runtime 无关的公共输入。
 */
public final class McpCallToolResultAdapter {
    private final ObjectMapper objectMapper;

    /**
     * 使用指定 JSON 映射器创建 MCP SDK 结果适配器。
     */
    public McpCallToolResultAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * 提取错误状态、结构化内容及文本内容，生成原始工具结果。
     */
    public RawToolResult adapt(McpSchema.CallToolResult result) {
        Objects.requireNonNull(result, "result must not be null");
        Map<String, Object> structured = result.structuredContent() == null
                ? Map.of()
                : objectMapper.convertValue(result.structuredContent(), new TypeReference<>() {
        });
        String text = result.content() == null ? "" : result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        return new RawToolResult(Boolean.TRUE.equals(result.isError()), structured, text);
    }
}
