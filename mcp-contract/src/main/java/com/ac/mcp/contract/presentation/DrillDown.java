package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DrillDown(boolean enabled, String dimension, String label, String promptTemplate) {
    public static DrillDown disabled() {
        return new DrillDown(false, null, null, null);
    }
}
