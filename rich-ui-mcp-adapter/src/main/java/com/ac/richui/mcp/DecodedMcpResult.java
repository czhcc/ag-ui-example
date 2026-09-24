package com.ac.richui.mcp;

import com.ac.mcp.contract.result.McpResult;

import java.util.Objects;

/**
 * MCP 结果按优先级和 Rich Result 契约识别后的解码结果。
 */
public record DecodedMcpResult(Kind kind, McpResult<?> result, String text) {
    /**
     * 校验结果种类与 Rich Result 是否匹配，并规范化空文本。
     */
    public DecodedMcpResult {
        Objects.requireNonNull(kind, "kind must not be null");
        text = text == null ? "" : text;
        if ((kind == Kind.RICH_RESULT || kind == Kind.BUSINESS_ERROR) && result == null) {
            throw new IllegalArgumentException(kind + " requires an McpResult");
        }
        if ((kind == Kind.MCP_ERROR || kind == Kind.INVALID_RICH_RESULT || kind == Kind.PLAIN_RESULT)
                && result != null) {
            throw new IllegalArgumentException(kind + " must not contain an McpResult");
        }
    }

    /**
     * 区分 MCP 错误、无效契约、业务失败、Rich Result 和普通结果。
     */
    public enum Kind {
        MCP_ERROR,
        INVALID_RICH_RESULT,
        BUSINESS_ERROR,
        RICH_RESULT,
        PLAIN_RESULT
    }
}
