package com.ac.agent.presentation.runtime;

import com.ac.agent.presentation.model.*;
import com.ac.mcp.contract.presentation.ViewHint;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PresentationMapper {
    public SurfaceSpec map(String resultRef, ViewHint view) {
        var type = switch (view.type()) {
            case "chart" -> UiComponentType.Chart;
            case "relation_graph" -> UiComponentType.RelationGraph;
            case "timeline" -> UiComponentType.Timeline;
            case "table" -> UiComponentType.Table;
            default -> throw new IllegalArgumentException("Unsupported view type: " + view.type());
        };
        Map<String, Object> props = new LinkedHashMap<>();
        if (view.subType() != null) props.put("subType", view.subType());
        if (view.title() != null) props.put("title", view.title());
        if (view.description() != null) props.put("description", view.description());
        props.put("encoding", view.mapping());
        props.put("options", view.options());
        return new SurfaceSpec("surface_" + UUID.randomUUID().toString().substring(0, 8), resultRef,
                List.of(new ComponentSpec(view.id(), type, Map.copyOf(props))));
    }
}
