package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.tool.ToolIdentity;

/**
 * Storage SPI for full tool results. Redis and database implementations belong
 * in infrastructure modules and must preserve these ownership checks.
 */
public interface ResultStore {

    ResultReference save(RunScope scope, ToolIdentity tool, McpResult<?> result);

    StoredMcpResult get(ResultReference reference, RunScope scope, AccessSubject subject);

    void remove(ResultReference reference, RunScope scope, AccessSubject subject);
}
