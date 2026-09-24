package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表格列映射；空列列表表示由展示端自动选择列。
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TableMapping(List<TableColumn> columns) implements ViewMapping {

    /**
     * 保存列定义的不可变副本。
     */
    public TableMapping {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }

    /**
     * 创建自动选择列的映射。
     */
    public static TableMapping auto() {
        return new TableMapping(List.of());
    }

    /**
     * 将旧版字段名到显示名称的映射转换为列定义。
     */
    public static TableMapping from(Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return auto();
        }
        return new TableMapping(mapping.entrySet().stream()
                .map(entry -> new TableColumn(entry.getKey(), entry.getValue()))
                .toList());
    }

    /**
     * 返回显式配置的表格字段名。
     */
    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(columns.stream().map(TableColumn::field).toArray(String[]::new));
    }
}
