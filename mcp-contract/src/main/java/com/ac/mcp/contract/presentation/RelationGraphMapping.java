package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Set;

/**
 * 关系图中源节点、目标节点及可选标签的字段映射。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationGraphMapping(
        String source,
        String target,
        String label,
        String sourceLabel) implements ViewMapping {

    /**
     * 校验必需字段，并规范化可选字段。
     */
    public RelationGraphMapping {
        source = MappingFields.required(source, "source");
        target = MappingFields.required(target, "target");
        label = MappingFields.optional(label);
        sourceLabel = MappingFields.optional(sourceLabel);
    }

    /**
     * 将字段名映射转换为关系图字段映射。
     */
    public static RelationGraphMapping from(Map<String, String> mapping) {
        return new RelationGraphMapping(
                mapping == null ? null : mapping.get("source"),
                mapping == null ? null : mapping.get("target"),
                mapping == null ? null : mapping.get("label"),
                mapping == null ? null : mapping.get("sourceLabel"));
    }

    /**
     * 返回关系图使用的业务字段名。
     */
    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(source, target, label, sourceLabel);
    }
}
