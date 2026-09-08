package com.ac.agent.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;

public interface McpClientManager {
    McpSyncClient getClient(String serverCode);
    void refresh(String serverCode);
    void remove(String serverCode);
    void closeAll();
}
