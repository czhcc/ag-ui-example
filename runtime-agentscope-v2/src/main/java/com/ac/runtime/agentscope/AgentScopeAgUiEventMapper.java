package com.ac.runtime.agentscope;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.event.RichRuntimeEvent;
import com.ac.richui.core.event.CustomRuntimeEvent;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.ac.richui.core.event.SurfaceCreated;
import com.ac.richui.core.text.BoundedText;
import com.ac.richui.core.text.SensitiveTextSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.event.CustomEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Converts official AgentScope AG-UI events into the runtime-neutral stream shared by both apps. */
public final class AgentScopeAgUiEventMapper {
    private final ObjectMapper mapper;
    private final SensitiveTextSanitizer sanitizer = new SensitiveTextSanitizer();

    public AgentScopeAgUiEventMapper(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public Optional<RichRuntimeEvent> map(AguiEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (event instanceof AguiEvent.Custom custom) return custom(custom.name(), custom.value());
        Map<String, Object> values = mapper.convertValue(event, new TypeReference<>() { });
        return switch (event.getType()) {
            case RUN_STARTED -> standard("run.started", Map.of());
            case RUN_FINISHED -> finished((AguiEvent.RunFinished) event);
            case RUN_ERROR -> standard("run.error", Map.of(
                    "errorCode", text(values, "code", "AGENTSCOPE_RUN_ERROR")));
            case TEXT_MESSAGE_START -> standard("text.message.start", Map.of());
            case TEXT_MESSAGE_CONTENT -> standard("text.message.content", Map.of(
                    "delta", text(values, "delta", "")));
            case TEXT_MESSAGE_END -> standard("text.message.end", Map.of());
            case TOOL_CALL_START -> standard("tool.call.start", Map.of(
                    "toolCallId", text(values, "toolCallId", "unknown"),
                    "toolName", text(values, "toolCallName", "unknown")));
            case TOOL_CALL_ARGS -> standard("tool.call.args", Map.of(
                    "toolCallId", text(values, "toolCallId", "unknown"),
                    "arguments", BoundedText.sanitizeAndLimit(
                            text(values, "delta", "{}"), sanitizer, 2_000)));
            case TOOL_CALL_END -> standard("tool.call.end", Map.of(
                    "toolCallId", text(values, "toolCallId", "unknown"),
                    "success", true));
            // Decorated MCP tools emit ac.rich-ui.tool-result after the native result. Ignoring the
            // native observation here prevents it from winning the shared translator's de-dup race.
            case TOOL_CALL_RESULT -> Optional.empty();
            default -> Optional.empty();
        };
    }

    public Optional<RichRuntimeEvent> map(CustomEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return custom(event.getName(), event.getValue());
    }

    private Optional<RichRuntimeEvent> custom(String name, Object value) {
        if ("ui.surface.create".equals(name)) {
            return Optional.of(new SurfaceCreated(mapper.convertValue(value, SurfaceSpec.class)));
        }
        if ("ac.rich-ui.tool-result".equals(name) && value instanceof Map<?, ?> source) {
            Map<String, Object> safe = new LinkedHashMap<>();
            for (String key : java.util.List.of("toolCallId", "toolName", "kind", "summary", "resultRef")) {
                Object attribute = source.get(key);
                if (attribute != null) safe.put(key, attribute);
            }
            return standard("tool.result", safe);
        }
        if (name.startsWith("subagent.") || "token_usage".equals(name)) {
            return Optional.of(new CustomRuntimeEvent(name, customValue(value)));
        }
        return Optional.empty();
    }

    private Map<String, Object> customValue(Object value) {
        if (value == null) return Map.of();
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            source.forEach((key, item) -> {
                if (key != null) normalized.put(String.valueOf(key), item);
            });
            return normalized;
        }
        return Map.of("value", value);
    }

    private Optional<RichRuntimeEvent> finished(AguiEvent.RunFinished event) {
        if (event.outcome() instanceof AguiEvent.RunFinishedInterruptOutcome outcome) {
            List<Map<String, Object>> approvals = outcome.interrupts().stream().map(interrupt -> {
                Map<String, Object> approval = new LinkedHashMap<>();
                approval.put("toolCallId", interrupt.toolCallId() == null
                        ? interrupt.id() : interrupt.toolCallId());
                Object toolName = interrupt.metadata() == null ? null : interrupt.metadata().get("toolName");
                approval.put("toolName", toolName == null ? interrupt.reason() : String.valueOf(toolName));
                approval.put("description", interrupt.message() == null
                        ? "User input is required" : interrupt.message());
                return Map.copyOf(approval);
            }).toList();
            return standard("run.interrupted", Map.of("approvals", approvals));
        }
        return standard("run.finished", Map.of());
    }

    private Optional<RichRuntimeEvent> standard(String type, Map<String, Object> attributes) {
        return Optional.of(new StandardRuntimeEvent(type, attributes));
    }

    private String text(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }
}
