package com.ac.agent.mcp.observation;

import com.ac.mcp.contract.result.McpResult;

public interface ObservationBuilder {
    String build(String toolName, String resultRef, McpResult<?> result);
}
