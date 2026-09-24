package com.ac.mcp.contract.presentation;

import java.util.Map;
import java.util.Set;

/**
 * 图表视图的分类字段和值字段映射。
 */
public record ChartMapping(String category, String value) implements ViewMapping {

    /**
     * 校验分类字段和值字段均非空。
     */
    public ChartMapping {
        category = MappingFields.required(category, "category");
        value = MappingFields.required(value, "value");
    }

    /**
     * 将字段名映射转换为图表字段映射。
     */
    public static ChartMapping from(Map<String, String> mapping) {
        return new ChartMapping(mapping == null ? null : mapping.get("category"),
                mapping == null ? null : mapping.get("value"));
    }

    /**
     * 返回图表使用的业务字段名。
     */
    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(category, value);
    }
}
