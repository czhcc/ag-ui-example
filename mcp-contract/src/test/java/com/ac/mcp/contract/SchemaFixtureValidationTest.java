package com.ac.mcp.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

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

    private JsonNode read(String resource) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing fixture " + resource);
            return mapper.readTree(stream);
        }
    }
}
