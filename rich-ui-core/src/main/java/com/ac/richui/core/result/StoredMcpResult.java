package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.tool.ToolIdentity;
import java.time.Instant;
import java.util.Objects;

/** Full result plus immutable ownership and expiry metadata. */
public record StoredMcpResult(
        ResultReference reference,
        RunScope scope,
        ToolIdentity tool,
        McpResult<?> result,
        long estimatedBytes,
        Instant createdAt,
        Instant expiresAt) {

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
