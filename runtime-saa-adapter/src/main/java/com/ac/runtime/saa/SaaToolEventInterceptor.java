package com.ac.runtime.saa;

import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RuntimeEventSink;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.ac.richui.core.text.BoundedText;
import com.ac.richui.core.text.SensitiveTextSanitizer;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Adds trusted tool-call identity and emits only safe tool lifecycle metadata. */
public final class SaaToolEventInterceptor extends ToolInterceptor {
    private static final String NAME = "SaaToolEvent";

    private final RuntimeEventSink events;

    public SaaToolEventInterceptor(RuntimeEventSink events) {
        this.events = Objects.requireNonNull(events, "events must not be null");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        var execution = request.getExecutionContext()
                .orElseThrow(() -> new IllegalStateException("SAA tool execution context is required"));
        String toolCallId = request.getToolCallId() == null || request.getToolCallId().isBlank()
                ? UUID.randomUUID().toString() : request.getToolCallId();
        RunScope scope = SaaRunMetadata.requireScope(execution.config(), toolCallId);
        Map<String, Object> context = new LinkedHashMap<>(request.getContext());
        context.putAll(SaaRunMetadata.toolContext(scope));
        ToolCallRequest enriched = new ToolCallRequest(
                request.getToolName(), request.getArguments(), toolCallId, context, execution);

        long started = System.nanoTime();
        events.publish(scope, new StandardRuntimeEvent("tool.call.start", Map.of(
                "toolCallId", toolCallId,
                "toolName", request.getToolName())));
        events.publish(scope, new StandardRuntimeEvent("tool.call.args", Map.of(
                "toolCallId", toolCallId,
                "toolName", request.getToolName(),
                "arguments", BoundedText.sanitizeAndLimit(
                        request.getArguments(), new SensitiveTextSanitizer(), 2_000))));
        try {
            ToolCallResponse response = handler.call(enriched);
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("toolCallId", toolCallId);
            attributes.put("toolName", request.getToolName());
            attributes.put("success", !response.isError());
            attributes.put("elapsedMs", elapsedMillis(started));
            Object resultRef = response.getMetadata().get("resultRef");
            if (resultRef != null) {
                attributes.put("resultRef", String.valueOf(resultRef));
            }
            events.publish(scope, new StandardRuntimeEvent("tool.call.end", attributes));
            events.publish(scope, new StandardRuntimeEvent("tool.result.fallback", Map.of(
                    "toolCallId", toolCallId,
                    "toolName", request.getToolName(),
                    "kind", "RUNTIME_RESULT",
                    "summary", BoundedText.sanitizeAndLimit(
                            response.getResult(), new SensitiveTextSanitizer(), 2_000))));
            return response;
        } catch (RuntimeException exception) {
            events.publish(scope, new StandardRuntimeEvent("tool.call.end", Map.of(
                    "toolCallId", toolCallId,
                    "toolName", request.getToolName(),
                    "success", false,
                    "elapsedMs", elapsedMillis(started),
                    "errorCode", exception.getClass().getSimpleName())));
            events.publish(scope, new StandardRuntimeEvent("tool.result.fallback", Map.of(
                    "toolCallId", toolCallId,
                    "toolName", request.getToolName(),
                    "kind", "RUNTIME_ERROR",
                    "summary", "Tool execution failed")));
            throw exception;
        }
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
