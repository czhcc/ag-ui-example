package com.ac.mcp.contract.presentation;

import com.ac.mcp.contract.result.McpResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhaseTwoBindingTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test void everyPhaseTwoFixtureDeserializesIntoTheExpectedViewType() throws Exception {
        Class<?>[] types = {MetricViewHint.class, EntityCardViewHint.class, TreeViewHint.class,
                HeatmapViewHint.class, RelationshipPathViewHint.class, EvidenceChainViewHint.class};
        String[] files = {"metric", "entity-card", "tree", "heatmap", "relationship-path", "evidence-chain"};
        for (int index = 0; index < files.length; index++) {
            try (InputStream input = getClass().getResourceAsStream("/schema/valid/" + files[index] + "-success.json")) {
                assertNotNull(input);
                McpResult<?> result = mapper.readValue(input, new TypeReference<McpResult<Object>>() {});
                assertEquals("1.2", result.specVersion());
                assertInstanceOf(types[index], result.presentation().views().get(0));
            }
        }
    }

    @Test void surfaceRevisionAppearsOnlyForProfileEleven() throws Exception {
        var component = new ComponentSpec("metric", "Metric", Map.of("subType", "single"));
        var legacy = new SurfaceSpec("old", "result-1", List.of(component));
        var next = new SurfaceSpec("ac.rich-ui", "1.1", "new", "result-2", List.of(component), 1);
        assertFalse(mapper.writeValueAsString(legacy).contains("revision"));
        assertTrue(mapper.writeValueAsString(next).contains("\"revision\":1"));
    }
}
