package com.ac.mcpapps.core;

import com.ac.richui.core.context.AccessSubject;
import java.util.Map;

/** Implemented by each client, never by the browser. Server credentials remain on the host. */
public interface McpAppTransport {
    Map<String, Object> readResource(String uri);
    Map<String, Object> callTool(String name, Map<String, Object> arguments, AccessSubject subject);
}
