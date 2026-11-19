package com.ac.mcpapps.web;

import com.ac.mcpapps.core.*;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.*;
import com.ac.richui.core.tool.*;
import org.slf4j.LoggerFactory;

/** Optional UI creation cannot turn an otherwise successful business query into a failed tool. */
public final class McpAppResultPublisher {
    private final McpAppService apps;
    private final RuntimeEventSink events;
    public McpAppResultPublisher(McpAppService apps, RuntimeEventSink events) { this.apps = apps; this.events = events; }
    public void publish(RunScope scope, ToolIdentity tool, ProcessedToolResult result, RawToolResult raw) {
        try { apps.create(scope, tool, result, raw).ifPresent(descriptor ->
                events.publish(scope, new CustomRuntimeEvent("mcp.app.create", descriptor))); }
        catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("MCP App unavailable server={} tool={} run={} code={}",
                    tool.serverCode(), tool.toolName(), scope.runId(), e instanceof McpAppException app ? app.code() : "APP_CREATE_FAILED");
        }
    }
}
