package com.ac.mcp.contract.presentation;

public record DrillDown(boolean enabled, String dimension, String label, String promptTemplate) {
    public static DrillDown disabled() {
        return new DrillDown(false, null, null, null);
    }
}
