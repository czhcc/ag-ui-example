package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 表格视图的展示建议，支持自动选列和显式列映射。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TableViewHint(String id, String type, String subType, String title, String description,
                            TableMapping mapping, Map<String, Object> options, Integer priority,
                            DrillDown drillDown)
        implements ViewHint {
    /**
     * 固定视图类型并补齐映射和可选配置的默认值。
     */
    public TableViewHint {
        type = "table";
        mapping = mapping == null ? TableMapping.auto() : mapping;
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }

    /**
     * 从旧版字符串映射创建表格视图建议。
     */
    public TableViewHint(String id, String type, String subType, String title, String description,
                         Map<String, String> mapping, Map<String, Object> options, Integer priority) {
        this(id, type, subType, title, description, TableMapping.from(mapping), options, priority, null);
    }
}
