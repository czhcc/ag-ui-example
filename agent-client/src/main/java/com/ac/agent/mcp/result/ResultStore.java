package com.ac.agent.mcp.result;

import com.ac.mcp.contract.result.McpResult;

public interface ResultStore {
    String save(String serverCode, String toolName, McpResult<?> result);
    String save(String serverCode, String toolName, String conversationId, String runId, McpResult<?> result);
    StoredMcpResult get(String resultRef);
    void remove(String resultRef);
}
