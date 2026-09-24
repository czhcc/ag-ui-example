package com.ac.richui.mcp;

import com.ac.richui.core.tool.RawToolResult;

/**
 * 将公共原始工具结果解码为明确的 MCP 结果种类。
 */
@FunctionalInterface
public interface McpResultDecoder {
    /**
     * 按契约和降级规则解码工具结果。
     */
    DecodedMcpResult decode(RawToolResult result);
}
