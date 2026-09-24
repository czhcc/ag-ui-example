package com.ac.richui.mcp;

import com.ac.mcp.contract.result.McpResult;
import java.util.Objects;

/** Result of applying MCP precedence and rich-envelope detection rules. */
public record DecodedMcpResult(Kind kind, McpResult<?> result, String text) {
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

    public enum Kind {
        MCP_ERROR,
        INVALID_RICH_RESULT,
        BUSINESS_ERROR,
        RICH_RESULT,
        PLAIN_RESULT
    }
}
