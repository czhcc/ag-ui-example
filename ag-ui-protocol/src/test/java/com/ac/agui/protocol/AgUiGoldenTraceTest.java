package com.ac.agui.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ac.mcp.contract.presentation.ComponentSpec;
import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.ac.richui.core.event.SurfaceCreated;
import com.agui.community.core.event.Event;
import com.agui.community.core.message.UserMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgUiGoldenTraceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void encodesOfficialModelsAsGoldenAgUiTrace() throws Exception {
        RunScope scope = new RunScope("tenant-1", "user-1", "thread-1", "run-1", null);
        AgUiRunAgentInput input = new AgUiRunAgentInput(
                "thread-1", "run-1", null, Map.of("route", "/sales"),
                List.of(new UserMessage("user-message-1", "show sales")),
                List.of(), List.of(), Map.of(), List.of());
        AgUiEventTranslator translator = new AgUiEventTranslator(scope, input, mapper);
        AgUiEventEncoder encoder = new AgUiEventEncoder(mapper);
        List<Event> events = new ArrayList<>();
        Instant instant = Instant.ofEpochMilli(1_000);
        events.addAll(translator.translate(new StandardRuntimeEvent("run.started", Map.of(), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent("text.message.start", Map.of(), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent(
                "text.message.content", Map.of("delta", "hello"), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent("tool.call.start", Map.of(
                "toolCallId", "tool-1", "toolName", "search"), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent("tool.call.args", Map.of(
                "toolCallId", "tool-1", "arguments", "{\"q\":\"sales\"}"), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent("tool.result", Map.of(
                "toolCallId", "tool-1", "kind", "RICH_RESULT",
                "summary", "1 row", "resultRef", "result://1",
                "resultJson", "DO_NOT_EXPOSE_FULL_RESULT",
                "structuredContent", Map.of("secret", "DO_NOT_EXPOSE_SECRET")), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent(
                "tool.call.end", Map.of("toolCallId", "tool-1"), instant)));
        events.addAll(translator.translate(new SurfaceCreated(new SurfaceSpec(
                "surface-1", "result://1", List.of(new ComponentSpec("c1", "Table", Map.of()))), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent("text.message.end", Map.of(), instant)));
        events.addAll(translator.translate(new StandardRuntimeEvent("run.finished", Map.of(), instant)));

        JsonNode actual = mapper.readTree("[" + String.join(",", events.stream().map(encoder::encode).toList()) + "]");
        assertFalse(actual.toString().contains("DO_NOT_EXPOSE_FULL_RESULT"));
        assertFalse(actual.toString().contains("DO_NOT_EXPOSE_SECRET"));
        try (InputStream stream = getClass().getResourceAsStream("/ag-ui/golden-trace.json")) {
            assertEquals(mapper.readTree(stream), actual);
        }
    }
}
