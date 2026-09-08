package com.ac.agent.mcp.result;

import com.ac.mcp.contract.result.McpResult;
import java.time.Instant;

public record StoredMcpResult(String resultRef, String serverCode, String toolName,
                              String conversationId, String runId, McpResult<?> result,
                              Instant createdAt, Instant expiresAt) { }
