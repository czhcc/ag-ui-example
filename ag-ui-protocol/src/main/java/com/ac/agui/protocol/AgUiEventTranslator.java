package com.ac.agui.protocol;

import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RichRuntimeEvent;
import com.ac.richui.core.event.CustomRuntimeEvent;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.ac.richui.core.event.SurfaceCreated;
import com.ac.richui.core.event.SurfaceUpdated;
import com.ac.richui.core.event.SurfaceUpdated;
import com.agui.community.core.event.CustomEvent;
import com.agui.community.core.event.Event;
import com.agui.community.core.event.RunErrorEvent;
import com.agui.community.core.event.RunFinishedEvent;
import com.agui.community.core.event.RunStartedEvent;
import com.agui.community.core.event.StateSnapshotEvent;
import com.agui.community.core.event.TextMessageContentEvent;
import com.agui.community.core.event.TextMessageEndEvent;
import com.agui.community.core.event.TextMessageStartEvent;
import com.agui.community.core.event.ToolCallArgsEvent;
import com.agui.community.core.event.ToolCallEndEvent;
import com.agui.community.core.event.ToolCallResultEvent;
import com.agui.community.core.event.ToolCallStartEvent;
import com.agui.community.core.interrupt.Interrupt;
import com.agui.community.core.interrupt.InterruptOutcome;
import com.agui.community.core.interrupt.SuccessOutcome;
import com.agui.community.core.message.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 将公共运行事件按消息、工具调用和终态关联后转换为官方 AG-UI 事件。
 */
public final class AgUiEventTranslator {
    private final RunScope scope;
    private final AgUiRunAgentInput input;
    private final ObjectMapper mapper;
    private final String messageId;
    private boolean runStarted;
    private boolean messageStarted;
    private boolean messageEnded;
    private boolean terminal;
    private final Map<String, Map<String, Object>> pendingToolResults = new LinkedHashMap<>();
    private final Set<String> endedToolCalls = new java.util.HashSet<>();
    private final Set<String> emittedToolResults = new java.util.HashSet<>();
    private final Set<SurfaceKey> emittedSurfaces = new java.util.HashSet<>();

    /**
     * 为指定运行范围和输入创建有状态的事件转换器。
     */
    public AgUiEventTranslator(RunScope scope, AgUiRunAgentInput input, ObjectMapper mapper) {
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
        this.input = Objects.requireNonNull(input, "input must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.messageId = scope.runId() + ":assistant";
    }

    /**
     * 转换单个内部事件，并按协议顺序返回零个或多个 AG-UI 事件。
     */
    public synchronized List<Event> translate(RichRuntimeEvent runtimeEvent) {
        Objects.requireNonNull(runtimeEvent, "runtimeEvent must not be null");
        if (terminal) return List.of();
        if (runtimeEvent instanceof SurfaceCreated created) {
            SurfaceKey key = new SurfaceKey(created.surface().dataRef(),
                    created.surface().components().stream().map(component -> component.id()).toList());
            if (!emittedSurfaces.add(key)) return List.of();
            return List.of(new CustomEvent("ui.surface.create", created.surface(), epoch(created.timestamp()), null));
        }
        if (runtimeEvent instanceof SurfaceUpdated updated) {
            return List.of(new CustomEvent("ui.surface.update", updated.surface(),
                    epoch(updated.timestamp()), null));
        }
        if (runtimeEvent instanceof SurfaceUpdated updated) {
            return List.of(new CustomEvent("ui.surface.update", updated.surface(),
                    epoch(updated.timestamp()), null));
        }
        if (runtimeEvent instanceof CustomRuntimeEvent custom) {
            return List.of(new CustomEvent(
                    custom.name(), custom.value(), epoch(custom.timestamp()), null));
        }
        if (!(runtimeEvent instanceof StandardRuntimeEvent event)) return List.of();
        long timestamp = epoch(event.timestamp());
        Map<String, Object> attributes = event.attributes();
        return switch (event.type()) {
            case "run.started", "run.resumed" -> started(timestamp);
            case "text.message.start" -> startMessage(timestamp);
            case "text.message.content" -> content(string(attributes, "delta", ""), timestamp);
            case "text.message.end" -> endMessage(timestamp);
            case "tool.call.start" -> List.of(new ToolCallStartEvent(
                    required(attributes, "toolCallId"), required(attributes, "toolName"),
                    messageId, timestamp, null));
            case "tool.call.args" -> List.of(new ToolCallArgsEvent(
                    required(attributes, "toolCallId"), string(attributes, "arguments", "{}"), timestamp, null));
            case "tool.call.end" -> endTool(attributes, timestamp);
            case "tool.result", "tool.result.fallback" -> resultTool(attributes, timestamp);
            case "run.finished" -> finish(new SuccessOutcome(), Map.of("status", "completed"), timestamp);
            case "run.cancelled" -> finish(new SuccessOutcome(), Map.of("status", "cancelled"), timestamp);
            case "run.interrupted" -> finish(interruptOutcome(attributes), Map.of("status", "interrupted"), timestamp);
            case "run.error" -> error(string(attributes, "errorCode", "AGENT_RUN_ERROR"), timestamp);
            default -> List.of();
        };
    }

    /**
     * 尚无终态时补发运行完成事件。
     */
    public synchronized List<Event> finishIfMissing(String status) {
        return terminal ? List.of() : finish(new SuccessOutcome(), Map.of("status", status), System.currentTimeMillis());
    }

    /**
     * 尚无终态时补发运行错误事件。
     */
    public synchronized List<Event> failIfMissing(String code) {
        return terminal ? List.of() : error(code, System.currentTimeMillis());
    }

    /**
     * 判断运行是否已发送完成或错误终态。
     */
    public synchronized boolean terminal() {
        return terminal;
    }

    private List<Event> started(long timestamp) {
        if (runStarted) return List.of();
        runStarted = true;
        List<Event> result = new ArrayList<>();
        result.add(new RunStartedEvent(scope.threadId(), scope.runId(), input.parentRunId(), null, timestamp, null));
        if (input.state() != null) result.add(new StateSnapshotEvent(input.state(), timestamp, null));
        return result;
    }

    private List<Event> startMessage(long timestamp) {
        if (messageStarted) return List.of();
        messageStarted = true;
        return List.of(new TextMessageStartEvent(messageId, Role.ASSISTANT, timestamp, null));
    }

    private List<Event> content(String delta, long timestamp) {
        if (delta.isEmpty()) return List.of();
        List<Event> result = new ArrayList<>();
        if (!messageStarted) result.addAll(startMessage(timestamp));
        result.add(new TextMessageContentEvent(messageId, delta, timestamp, null));
        return result;
    }

    private List<Event> endMessage(long timestamp) {
        if (!messageStarted || messageEnded) return List.of();
        messageEnded = true;
        return List.of(new TextMessageEndEvent(messageId, timestamp, null));
    }

    private ToolCallResultEvent toolResult(Map<String, Object> attributes, long timestamp) {
        String toolCallId = required(attributes, "toolCallId");
        Map<String, Object> safe = new LinkedHashMap<>();
        copy(attributes, safe, "kind");
        copy(attributes, safe, "summary");
        copy(attributes, safe, "resultRef");
        try {
            return new ToolCallResultEvent(toolCallId + ":result", toolCallId,
                    mapper.writeValueAsString(safe), Role.TOOL, timestamp, null);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot encode safe tool result", exception);
        }
    }

    private List<Event> endTool(Map<String, Object> attributes, long timestamp) {
        String toolCallId = required(attributes, "toolCallId");
        endedToolCalls.add(toolCallId);
        List<Event> result = new ArrayList<>();
        result.add(new ToolCallEndEvent(toolCallId, timestamp, null));
        Map<String, Object> pending = pendingToolResults.remove(toolCallId);
        if (pending != null && emittedToolResults.add(toolCallId)) {
            result.add(toolResult(pending, timestamp));
        }
        return result;
    }

    private List<Event> resultTool(Map<String, Object> attributes, long timestamp) {
        String toolCallId = required(attributes, "toolCallId");
        if (emittedToolResults.contains(toolCallId)) return List.of();
        if (!endedToolCalls.contains(toolCallId)) {
            pendingToolResults.put(toolCallId, Map.copyOf(attributes));
            return List.of();
        }
        emittedToolResults.add(toolCallId);
        return List.of(toolResult(attributes, timestamp));
    }

    private List<Event> finish(com.agui.community.core.interrupt.RunOutcome outcome,
                               Object result, long timestamp) {
        List<Event> events = new ArrayList<>();
        if (!runStarted) events.addAll(started(timestamp));
        events.addAll(endMessage(timestamp));
        terminal = true;
        events.add(new RunFinishedEvent(scope.threadId(), scope.runId(), outcome, result, timestamp, null));
        return events;
    }

    private List<Event> error(String code, long timestamp) {
        List<Event> events = new ArrayList<>();
        if (!runStarted) events.addAll(started(timestamp));
        events.addAll(endMessage(timestamp));
        terminal = true;
        events.add(new RunErrorEvent("Agent run failed", code, timestamp, null));
        return events;
    }

    private InterruptOutcome interruptOutcome(Map<String, Object> attributes) {
        Object value = attributes.get("approvals");
        if (!(value instanceof List<?> approvals)) return new InterruptOutcome(List.of());
        List<Interrupt> interrupts = new ArrayList<>();
        for (Object item : approvals) {
            if (!(item instanceof Map<?, ?> approval)) continue;
            String id = String.valueOf(approval.get("toolCallId"));
            String toolName = String.valueOf(approval.get("toolName"));
            String description = approval.get("description") == null
                    ? "Approval required for " + toolName : String.valueOf(approval.get("description"));
            interrupts.add(new Interrupt(id, "tool_call", description, id, null, null,
                    Map.of("toolName", toolName)));
        }
        return new InterruptOutcome(interrupts);
    }

    private void copy(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.get(key) != null) target.put(key, source.get(key));
    }

    private String required(Map<String, Object> attributes, String key) {
        String value = string(attributes, key, null);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing runtime attribute: " + key);
        return value;
    }

    private String string(Map<String, Object> attributes, String key, String fallback) {
        Object value = attributes.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private long epoch(java.time.Instant instant) {
        return instant == null ? System.currentTimeMillis() : instant.toEpochMilli();
    }

    private record SurfaceKey(String dataRef, List<String> componentIds) {
    }
}
