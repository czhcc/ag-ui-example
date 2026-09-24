package com.ac.agui.protocol;

import com.agui.community.core.event.CustomEvent;
import com.agui.community.core.event.Event;
import com.agui.community.core.event.RunErrorEvent;
import com.agui.community.core.event.RunFinishedEvent;
import com.agui.community.core.event.RunStartedEvent;
import com.agui.community.core.event.StateDeltaEvent;
import com.agui.community.core.event.StateSnapshotEvent;
import com.agui.community.core.event.TextMessageContentEvent;
import com.agui.community.core.event.TextMessageEndEvent;
import com.agui.community.core.event.TextMessageStartEvent;
import com.agui.community.core.event.ToolCallArgsEvent;
import com.agui.community.core.event.ToolCallEndEvent;
import com.agui.community.core.event.ToolCallResultEvent;
import com.agui.community.core.event.ToolCallStartEvent;
import com.agui.community.core.interrupt.InterruptOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * 将官方 AG-UI 事件编码为使用协议字段名和小写角色值的紧凑 JSON。
 */
public final class AgUiEventEncoder {
    private final ObjectMapper mapper;

    /**
     * 使用指定 JSON 映射器创建事件编码器。
     */
    public AgUiEventEncoder(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * 按 AG-UI 线协议编码指定事件。
     */
    public String encode(Event event) {
        ObjectNode json = mapper.createObjectNode();
        json.put("type", event.type().value());
        if (event.timestamp() != null) json.put("timestamp", event.timestamp());
        if (event instanceof RunStartedEvent value) {
            json.put("threadId", value.threadId());
            json.put("runId", value.runId());
            put(json, "parentRunId", value.parentRunId());
            set(json, "input", value.input());
        } else if (event instanceof RunFinishedEvent value) {
            json.put("threadId", value.threadId());
            json.put("runId", value.runId());
            if (value.outcome() != null) {
                ObjectNode outcome = mapper.valueToTree(value.outcome());
                outcome.put("type", value.outcome().type().value());
                if (value.outcome() instanceof InterruptOutcome interrupt) {
                    outcome.set("interrupts", mapper.valueToTree(interrupt.interrupts()));
                }
                json.set("outcome", outcome);
            }
            set(json, "result", value.result());
        } else if (event instanceof RunErrorEvent value) {
            json.put("message", value.message());
            put(json, "code", value.code());
        } else if (event instanceof TextMessageStartEvent value) {
            json.put("messageId", value.messageId());
            json.put("role", value.role().value());
        } else if (event instanceof TextMessageContentEvent value) {
            json.put("messageId", value.messageId());
            json.put("delta", value.delta());
        } else if (event instanceof TextMessageEndEvent value) {
            json.put("messageId", value.messageId());
        } else if (event instanceof ToolCallStartEvent value) {
            json.put("toolCallId", value.toolCallId());
            json.put("toolCallName", value.toolCallName());
            put(json, "parentMessageId", value.parentMessageId());
        } else if (event instanceof ToolCallArgsEvent value) {
            json.put("toolCallId", value.toolCallId());
            json.put("delta", value.delta());
        } else if (event instanceof ToolCallEndEvent value) {
            json.put("toolCallId", value.toolCallId());
        } else if (event instanceof ToolCallResultEvent value) {
            json.put("messageId", value.messageId());
            json.put("toolCallId", value.toolCallId());
            json.put("content", value.content());
            if (value.role() != null) json.put("role", value.role().value());
        } else if (event instanceof StateSnapshotEvent value) {
            set(json, "snapshot", value.snapshot());
        } else if (event instanceof StateDeltaEvent value) {
            json.set("delta", mapper.valueToTree(value.delta()));
        } else if (event instanceof CustomEvent value) {
            json.put("name", value.name());
            set(json, "value", value.value());
        } else {
            throw new IllegalArgumentException("Unsupported AG-UI event: " + event.getClass().getName());
        }
        try {
            return mapper.writeValueAsString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot encode AG-UI event", exception);
        }
    }

    private void put(ObjectNode target, String name, String value) {
        if (value != null) target.put(name, value);
    }

    private void set(ObjectNode target, String name, Object value) {
        if (value != null) target.set(name, mapper.valueToTree(value));
    }
}
