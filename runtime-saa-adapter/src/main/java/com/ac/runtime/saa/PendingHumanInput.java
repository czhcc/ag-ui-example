package com.ac.runtime.saa;

import java.util.List;

public record PendingHumanInput(String runId, List<ToolApproval> approvals) {
    public record ToolApproval(String toolCallId, String toolName, String arguments, String description) { }
}
