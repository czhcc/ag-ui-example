package com.ac.mcp.contract.presentation;

import java.util.Map;

public record TimelineViewHint(String id, String type, String subType, String title, String description,
                               Map<String, String> mapping, Map<String, Object> options, Integer priority)
        implements ViewHint {
    public TimelineViewHint { type = "timeline"; mapping = mapping == null ? Map.of() : Map.copyOf(mapping); options = options == null ? Map.of() : Map.copyOf(options); priority = priority == null ? 0 : priority; }
}
