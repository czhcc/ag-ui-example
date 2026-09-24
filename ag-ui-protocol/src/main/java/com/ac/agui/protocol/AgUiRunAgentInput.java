package com.ac.agui.protocol;

import com.agui.community.core.agent.Context;
import com.agui.community.core.agent.RunAgentInput;
import com.agui.community.core.interrupt.Resume;
import com.agui.community.core.message.Message;
import com.agui.community.core.tool.Tool;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete wire input, including parentRunId which java-core 0.1.1 does not yet expose. */
public record AgUiRunAgentInput(
        String threadId,
        String runId,
        String parentRunId,
        Object state,
        List<Message> messages,
        List<Tool> tools,
        List<Context> context,
        Object forwardedProps,
        List<Resume> resume) {

    public AgUiRunAgentInput {
        threadId = requireText(threadId, "threadId");
        runId = requireText(runId, "runId");
        parentRunId = normalize(parentRunId);
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
        context = context == null ? List.of() : List.copyOf(context);
        resume = resume == null ? List.of() : List.copyOf(resume);
    }

    public RunAgentInput officialInput() {
        return new RunAgentInput(threadId, runId, state, messages, tools, context, forwardedProps, resume);
    }

    /** Only these values may influence presentation; identity and authorization never come from the body. */
    public Map<String, Object> trustedForwardedProps() {
        if (!(forwardedProps instanceof Map<?, ?> source)) return Map.of();
        var safe = new java.util.LinkedHashMap<String, Object>();
        for (String key : List.of("locale", "timeZone", "clientName")) {
            Object value = source.get(key);
            if (value instanceof String text && !text.isBlank() && text.length() <= 128) {
                safe.put(key, text);
            }
        }
        return Map.copyOf(safe);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
