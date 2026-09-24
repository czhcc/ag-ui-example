package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.tool.ToolIdentity;

import java.time.Instant;
import java.util.Objects;

/**
 * 完整 MCP 结果及其不可变的归属、大小和有效期信息。
 */
public record StoredMcpResult(
        ResultReference reference,
        RunScope scope,
        ToolIdentity tool,
        McpResult<?> result,
        long estimatedBytes,
        Instant createdAt,
        Instant expiresAt) {

    /**
     * 校验结果引用、归属信息、大小和时间戳。
     */
    public StoredMcpResult {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        Objects.requireNonNull(result, "result must not be null");
        if (estimatedBytes < 1) throw new IllegalArgumentException("estimatedBytes must be positive");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
