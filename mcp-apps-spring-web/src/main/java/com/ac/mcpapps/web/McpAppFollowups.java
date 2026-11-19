package com.ac.mcpapps.web;

import com.ac.mcpapps.core.McpAppService;
import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import com.agui.community.core.agent.Context;
import java.util.*;

/** One common input adapter used by both runtime hosts. Never trusts browser result references. */
public final class McpAppFollowups {
    private final McpAppService apps;
    public McpAppFollowups(McpAppService apps) { this.apps = apps; }
    public AgUiRunAgentInput enrich(AgUiRunAgentInput input, RunScope scope) {
        if (!(input.forwardedProps() instanceof Map<?, ?> props) || !props.containsKey("appFollowupRef")) return input;
        Object ref = props.get("appFollowupRef");
        if (!(ref instanceof String text) || text.length() > 256) throw new com.ac.mcpapps.core.McpAppException("APP_FOLLOWUP_INVALID");
        List<Context> context = new ArrayList<>(input.context());
        context.add(new Context("Authorized MCP App selection", apps.consumeFollowup(text, scope)));
        return new AgUiRunAgentInput(input.threadId(), input.runId(), input.parentRunId(), input.state(),
                input.messages(), input.tools(), context, input.trustedForwardedProps(), input.resume());
    }
}
