package com.ac.mcpapps.core;

import com.ac.richui.core.context.AccessSubject;
import java.util.Set;

/** Evaluated on every operation. Integrators may intersect business entitlements here. */
public interface McpAppPolicy {
    boolean allows(AccessSubject subject, String server, String tool);
    static McpAppPolicy allowlist(Set<String> tools) {
        Set<String> copy = Set.copyOf(tools);
        return (subject, server, tool) -> copy.contains(server + ":" + tool);
    }
}
