package com.ac.richui.core.tool;

import com.ac.richui.core.observation.AgentObservation;
import com.ac.richui.core.result.ResultReference;
import java.util.Objects;

/** Safe result returned by the shared processor to a runtime adapter. */
public record ProcessedToolResult(
        Kind kind,
        AgentObservation observation,
        ResultReference resultReference,
        String publicSummary) {

    public ProcessedToolResult {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(observation, "observation must not be null");
        publicSummary = publicSummary == null ? "" : publicSummary;
        if (kind == Kind.RICH_RESULT && resultReference == null) {
            throw new IllegalArgumentException("RICH_RESULT requires a resultReference");
        }
    }

    public enum Kind {
        MCP_ERROR,
        BUSINESS_ERROR,
        RICH_RESULT,
        PLAIN_RESULT
    }
}
