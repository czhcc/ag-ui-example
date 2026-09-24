package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.ComponentSpec;
import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.mcp.contract.presentation.ViewHint;
import com.ac.richui.core.result.ResultReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 将已校验的视图建议映射为带版本的 ac.rich-ui Surface 描述。
 */
public final class PresentationMapper {

    /**
     * 为指定结果引用和视图创建声明式 Surface。
     */
    public SurfaceSpec map(ResultReference reference, ViewHint view) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(view, "view must not be null");
        String type = switch (view.type()) {
            case "chart" -> "Chart";
            case "relation_graph" -> "RelationGraph";
            case "timeline" -> "Timeline";
            case "table" -> "Table";
            default -> throw new IllegalArgumentException("Unsupported view type: " + view.type());
        };
        Map<String, Object> props = new LinkedHashMap<>();
        if (view.subType() != null) props.put("subType", view.subType());
        if (view.title() != null) props.put("title", view.title());
        if (view.description() != null) props.put("description", view.description());
        props.put("encoding", view.mapping());
        props.put("options", view.options());
        if (view.drillDown() != null && view.drillDown().enabled()) {
            Map<String, Object> drillDown = new LinkedHashMap<>();
            drillDown.put("enabled", true);
            if (view.drillDown().dimension() != null) drillDown.put("dimension", view.drillDown().dimension());
            drillDown.put("label", view.drillDown().label() == null ? "深入分析" : view.drillDown().label());
            if (view.drillDown().promptTemplate() != null) {
                drillDown.put("promptTemplate", view.drillDown().promptTemplate());
            }
            props.put("drillDown", Map.copyOf(drillDown));
        }
        String surfaceId = "surface_" + UUID.randomUUID().toString().replace("-", "");
        return new SurfaceSpec(surfaceId, reference.value(),
                List.of(new ComponentSpec(view.id(), type, Map.copyOf(props))));
    }
}
