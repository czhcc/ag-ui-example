package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 时间线视图的展示建议，包含字段映射和可选下钻配置。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimelineViewHint(String id, String type, String subType, String title, String description,
                               TimelineMapping mapping, Map<String, Object> options, Integer priority,
                               DrillDown drillDown)
        implements ViewHint {
    /**
     * 固定视图类型并补齐可选配置的默认值。
     */
    public TimelineViewHint {
        type = "timeline";
        mapping = java.util.Objects.requireNonNull(mapping, "mapping must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }

    /**
     * 从旧版字符串映射创建时间线视图建议。
     */
    public TimelineViewHint(String id, String type, String subType, String title, String description,
                            Map<String, String> mapping, Map<String, Object> options, Integer priority) {
        this(id, type, subType, title, description, TimelineMapping.from(mapping), options, priority, null);
    }
}
