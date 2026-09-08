package com.ac.agent.presentation.runtime;

import com.ac.agent.mcp.result.*;
import com.ac.agent.streaming.DefaultAgentEventBus;
import com.ac.mcp.contract.presentation.*;
import com.ac.mcp.contract.result.McpResult;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresentationRuntimeTest {
    @Test void createsSurfaceFromStoredView() {
        var store = new InMemoryResultStore(Duration.ofMinutes(30), new ResultRefGenerator());
        var view = new ChartViewHint("city-stat", "chart", "bar", "城市统计", null,
                Map.of("category", "city", "value", "count"), Map.of(), 1);
        var result = McpResult.success(List.of(Map.of("city", "北京", "count", 18)), null,
                PresentationHint.recommended(view), null);
        var ref = store.save("knowledge", "kg_activity_statistics", result);
        var runtime = new PresentationRuntime(store, new PresentationValidator(JsonMapper.builder().build()),
                new PresentationMapper(), new DefaultAgentEventBus());
        assertEquals(ref, runtime.render(ref, "city-stat").dataRef());
    }
}
