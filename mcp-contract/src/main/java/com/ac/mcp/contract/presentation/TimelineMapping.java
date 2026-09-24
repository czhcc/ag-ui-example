package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Set;

/**
 * 时间线中时间、标题及可选描述和分组的字段映射。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimelineMapping(
        String time,
        String title,
        String description,
        String group) implements ViewMapping {

    /**
     * 校验必需字段，并规范化可选字段。
     */
    public TimelineMapping {
        time = MappingFields.required(time, "time");
        title = MappingFields.required(title, "title");
        description = MappingFields.optional(description);
        group = MappingFields.optional(group);
    }

    /**
     * 将字段名映射转换为时间线字段映射。
     */
    public static TimelineMapping from(Map<String, String> mapping) {
        return new TimelineMapping(
                mapping == null ? null : mapping.get("time"),
                mapping == null ? null : mapping.get("title"),
                mapping == null ? null : mapping.get("description"),
                mapping == null ? null : mapping.get("group"));
    }

    /**
     * 返回时间线使用的业务字段名。
     */
    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(time, title, description, group);
    }
}
