package com.ac.richui.mcp;

import com.ac.richui.core.tool.RawToolResult;

@FunctionalInterface
public interface McpResultDecoder {
    DecodedMcpResult decode(RawToolResult result);
}
