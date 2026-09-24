package com.ac.agui.protocol;

import com.agui.community.core.agent.Context;
import com.agui.community.core.agent.RunAgentInput;
import com.agui.community.core.interrupt.Resume;
import com.agui.community.core.message.Message;
import com.agui.community.core.tool.Tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 完整的 AG-UI 线协议输入，补充 java-core 0.1.1 尚未提供的 parentRunId。
 */
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

    /**
     * 校验运行标识并保存各列表字段的不可变副本。
     */
    public AgUiRunAgentInput {
        threadId = requireText(threadId, "threadId");
        runId = requireText(runId, "runId");
        parentRunId = normalize(parentRunId);
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
        context = context == null ? List.of() : List.copyOf(context);
        resume = resume == null ? List.of() : List.copyOf(resume);
    }

    /**
     * 转换为官方 Java SDK 的运行输入类型。
     */
    public RunAgentInput officialInput() {
        return new RunAgentInput(threadId, runId, state, messages, tools, context, forwardedProps, resume);
    }

    /**
     * 仅保留可影响展示的白名单转发参数，不从请求正文获取身份或授权信息。
     */
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
