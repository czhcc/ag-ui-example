package com.ac.agui.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ac.mcp.contract.presentation.ComponentSpec;
import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.ac.richui.core.event.SurfaceCreated;
import com.agui.community.core.event.Event;
import com.agui.community.core.message.UserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 验证运行、消息和工具调用事件的顺序与关联。 */
class AgUiEventSequenceTest {
    @Test
    void emitsOneCanonicalSequenceAndExactlyOneTerminalEvent() {
        var input = new AgUiRunAgentInput("thread", "run", null, Map.of("page", 1),
                List.of(new UserMessage("user-1", "hello")), List.of(), List.of(), Map.of(), List.of());
        var translator = new AgUiEventTranslator(
                new RunScope("tenant", "user", "thread", "run", null), input, new ObjectMapper());
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        List<Event> events = new ArrayList<>();

        events.addAll(translator.translate(new StandardRuntimeEvent("run.started", Map.of(), now)));
        events.addAll(translator.translate(new StandardRuntimeEvent(
                "text.message.content", Map.of("delta", "answer"), now)));
        events.addAll(translator.translate(new StandardRuntimeEvent("tool.call.start", Map.of(
                "toolCallId", "call-1", "toolName", "search"), now)));
        events.addAll(translator.translate(new StandardRuntimeEvent("tool.result", Map.of(
                "toolCallId", "call-1", "kind", "RICH_RESULT",
                "summary", "one row", "resultRef", "result-1"), now)));
        events.addAll(translator.translate(new StandardRuntimeEvent(
                "tool.call.end", Map.of("toolCallId", "call-1"), now)));
        events.addAll(translator.translate(new SurfaceCreated(new SurfaceSpec(
                "surface-1", "result-1",
                List.of(new ComponentSpec("table-1", "Table", Map.of("encoding", Map.of())))), now)));
        events.addAll(translator.translate(new StandardRuntimeEvent("run.finished", Map.of(), now)));
        events.addAll(translator.translate(new StandardRuntimeEvent(
                "run.error", Map.of("errorCode", "LATE"), now)));

        assertEquals(List.of(
                "RUN_STARTED", "STATE_SNAPSHOT",
                "TEXT_MESSAGE_START", "TEXT_MESSAGE_CONTENT",
                "TOOL_CALL_START", "TOOL_CALL_END", "TOOL_CALL_RESULT",
                "CUSTOM", "TEXT_MESSAGE_END", "RUN_FINISHED"),
                events.stream().map(event -> event.type().value()).toList());
        assertEquals(1, events.stream().filter(event ->
                event.type().value().equals("RUN_FINISHED")
                        || event.type().value().equals("RUN_ERROR")).count());
    }

    @Test
    void repeatedRenderOfTheSameResultViewEmitsOneSurface() {
        var input = new AgUiRunAgentInput("thread", "run", null, Map.of(),
                List.of(new UserMessage("user-1", "show data")), List.of(), List.of(), Map.of(), List.of());
        var translator = new AgUiEventTranslator(
                new RunScope("tenant", "user", "thread", "run", null), input, new ObjectMapper());
        var component = new ComponentSpec("city-stat", "Chart", Map.of());
        var first = new SurfaceCreated(new SurfaceSpec("surface-1", "result-1", List.of(component)));
        var repeated = new SurfaceCreated(new SurfaceSpec("surface-2", "result-1", List.of(component)));
        var differentView = new SurfaceCreated(new SurfaceSpec("surface-3", "result-1",
                List.of(new ComponentSpec("activity-timeline", "Timeline", Map.of()))));

        assertEquals(1, translator.translate(first).size());
        assertEquals(0, translator.translate(repeated).size());
        assertEquals(1, translator.translate(differentView).size());
    }
}
