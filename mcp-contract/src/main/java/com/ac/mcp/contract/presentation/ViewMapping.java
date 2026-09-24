package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

/**
 * 契约校验和渲染端共用的强类型字段映射。
 */
public sealed interface ViewMapping
        permits ChartMapping, TableMapping, TimelineMapping, RelationGraphMapping {

    /**
     * 返回映射引用的业务字段名，用于校验数据与视图的一致性。
     */
    @JsonIgnore
    Set<String> referencedFields();
}
