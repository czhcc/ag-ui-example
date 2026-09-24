package com.ac.runtime.agentscope;

import com.ac.richui.core.event.CustomRuntimeEvent;
import com.ac.richui.core.event.SurfaceCreated;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agui.event.AguiEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AgentScopeAgUiEventMapperTest {
    private final AgentScopeAgUiEventMapper mapper = new AgentScopeAgUiEventMapper(new ObjectMapper());

    @Test
    void keepsValidatedRichUiProfileAcrossTheOfficialAdapterBoundary() {
        var value = Map.<String, Object>of(
                "profile", "ac.rich-ui",
                "profileVersion", "1.0",
                "surfaceId", "surface-1",
                "dataRef", "result-1",
                "components", List.of(Map.of(
                        "id", "table-1", "type", "Table", "props", Map.of())));
        var mapped = mapper.map(new AguiEvent.Custom(
                "thread", "run", "ui.surface.create", value)).orElseThrow();

        SurfaceCreated surface = assertInstanceOf(SurfaceCreated.class, mapped);
        assertEquals("ac.rich-ui", surface.surface().profile());
        assertEquals("1.0", surface.surface().profileVersion());
    }

    @Test
    void preservesScopedSubagentCustomEvents() {
        var mapped = mapper.map(new AguiEvent.Custom(
                "thread", "run", "subagent.task.completed", Map.of("taskId", "task-1")))
                .orElseThrow();
        CustomRuntimeEvent custom = assertInstanceOf(CustomRuntimeEvent.class, mapped);
        assertEquals("subagent.task.completed", custom.name());
        assertEquals("task-1", custom.value().get("taskId"));
    }

    @Test
    void wrapsScalarCustomEventValues() {
        var mapped = mapper.map(new AguiEvent.Custom(
                "thread", "run", "token_usage", 42)).orElseThrow();

        CustomRuntimeEvent custom = assertInstanceOf(CustomRuntimeEvent.class, mapped);
        assertEquals(42, custom.value().get("value"));
    }
}
