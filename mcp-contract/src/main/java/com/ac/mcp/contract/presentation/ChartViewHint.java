package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 图表视图的展示建议，包含字段映射、选项和下钻配置。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChartViewHint(String id, String type, String subType, String title, String description,
                            ChartMapping mapping, Map<String, Object> options, Integer priority,
                            DrillDown drillDown)
        implements ViewHint {
    /**
     * 固定视图类型并补齐可选配置的默认值。
     */
    public ChartViewHint {
        type = "chart";
        mapping = java.util.Objects.requireNonNull(mapping, "mapping must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }

    /**
     * 从旧版字符串映射创建图表视图建议。
     */
    public ChartViewHint(String id, String type, String subType, String title, String description,
                         Map<String, String> mapping, Map<String, Object> options, Integer priority) {
        this(id, type, subType, title, description, ChartMapping.from(mapping), options, priority, null);
    }
}
