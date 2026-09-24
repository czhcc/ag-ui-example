package com.ac.richui.core.observation;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.tool.ToolIdentity;

/** Builds model-safe observations without copying full business data. */
@FunctionalInterface
public interface ObservationBuilder {

    AgentObservation build(
            RunScope scope,
            ToolIdentity tool,
            ResultReference reference,
            McpResult<?> result);
}
