package com.ac.mcp.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ac.mcp.contract.presentation.ComponentSpec;
import com.ac.mcp.contract.presentation.MetricMapping;
import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 按清单验证 JSON Schema 的有效、无效和兼容版本样例。 */
class SchemaFixtureValidationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaFactory schemas = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    @Test
    void everyDeclaredValidAndInvalidFixtureMatchesItsSchema() throws IOException {
        JsonNode manifest = read("/schema/manifest.json");
        Map<String, JsonSchema> cache = new HashMap<>();
        for (JsonNode testCase : manifest.path("cases")) {
            String schemaName = testCase.path("schema").asText();
            String path = testCase.path("path").asText();
            boolean expected = testCase.path("valid").asBoolean();
            JsonSchema schema = cache.computeIfAbsent(schemaName, this::loadSchema);
            var errors = schema.validate(read("/schema/" + path));
            assertEquals(expected, errors.isEmpty(),
                    () -> path + " expected valid=" + expected + " but errors were " + errors);
        }
    }

    private JsonSchema loadSchema(String name) {
        String resource = "/schema/" + name + ".schema.json";
        InputStream stream = getClass().getResourceAsStream(resource);
        assertNotNull(stream, "Missing schema " + resource);
        try (stream) {
            return schemas.getSchema(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + resource, exception);
        }
    }

    @Test
    void serializedMetricSurfaceOmitsUnsetOptionalEncodingAndMatchesSchema() {
        var mapping = new MetricMapping("label", "value", "unit", null);
        var surface = new SurfaceSpec("ac.rich-ui", "1.1", "metric-surface", "result-1",
                List.of(new ComponentSpec("knowledge-overview", "Metric", Map.of(
                        "subType", "single", "encoding", mapping, "options", Map.of("limit", 12)))), 1);
        JsonNode encoded = mapper.valueToTree(Map.of("type", "CUSTOM", "name", "ui.surface.create",
                "value", surface));
        assertEquals(false, encoded.path("value").path("components").get(0)
                .path("props").path("encoding").has("change"));
        assertEquals(java.util.Set.of(), loadSchema("ac-rich-ui-profile").validate(encoded));
    }

    private JsonNode read(String resource) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing fixture " + resource);
            return mapper.readTree(stream);
        }
    }
}
