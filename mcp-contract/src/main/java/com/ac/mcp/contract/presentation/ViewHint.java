package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

/**
 * 各类视图建议的公共契约，通过 type 区分具体视图。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChartViewHint.class, name = "chart"),
        @JsonSubTypes.Type(value = RelationGraphViewHint.class, name = "relation_graph"),
        @JsonSubTypes.Type(value = TimelineViewHint.class, name = "timeline"),
        @JsonSubTypes.Type(value = TableViewHint.class, name = "table"),
        @JsonSubTypes.Type(value = MetricViewHint.class, name = "metric"),
        @JsonSubTypes.Type(value = EntityCardViewHint.class, name = "entity_card"),
        @JsonSubTypes.Type(value = TreeViewHint.class, name = "tree"),
        @JsonSubTypes.Type(value = HeatmapViewHint.class, name = "heatmap"),
        @JsonSubTypes.Type(value = RelationshipPathViewHint.class, name = "relationship_path"),
        @JsonSubTypes.Type(value = EvidenceChainViewHint.class, name = "evidence_chain")
})
public sealed interface ViewHint permits ChartViewHint, RelationGraphViewHint, TimelineViewHint, TableViewHint,
        MetricViewHint, EntityCardViewHint, TreeViewHint, HeatmapViewHint, RelationshipPathViewHint, EvidenceChainViewHint {
    /**
     * 返回视图标识。
     */
    String id();

    /**
     * 返回视图类型。
     */
    String type();

    /**
     * 返回视图子类型。
     */
    String subType();

    /**
     * 返回视图标题。
     */
    String title();

    /**
     * 返回视图描述。
     */
    String description();

    /**
     * 返回该视图的数据字段映射。
     */
    ViewMapping mapping();

    /**
     * 返回视图渲染选项。
     */
    Map<String, Object> options();

    /**
     * 返回视图展示优先级。
     */
    Integer priority();

    /**
     * 返回下钻配置。
     */
    DrillDown drillDown();
}
