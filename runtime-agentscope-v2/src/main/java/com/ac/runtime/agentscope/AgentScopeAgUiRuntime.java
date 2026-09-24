package com.ac.runtime.agentscope;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RuntimeEventSink;
import com.agui.community.core.message.AssistantMessage;
import com.agui.community.core.message.Message;
import com.agui.community.core.message.ToolMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.model.AguiContext;
import io.agentscope.core.agui.model.AguiFunctionCall;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.AguiResume;
import io.agentscope.core.agui.model.AguiTool;
import io.agentscope.core.agui.model.AguiToolCall;
import io.agentscope.core.agui.model.RunAgentInput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

/** Executes AgentScope through its official AguiAgentAdapter and republishes the shared profile. */
public final class AgentScopeAgUiRuntime {
    private final AguiAgentAdapter adapter;
    private final AgentScopeAgUiEventMapper events;
    private final RuntimeEventSink sink;
    private final ConcurrentMap<RunKey, Subscription> active = new ConcurrentHashMap<>();

    public AgentScopeAgUiRuntime(Agent agent, RuntimeEventSink sink, ObjectMapper mapper) {
        this.adapter = new AguiAgentAdapter(
                Objects.requireNonNull(agent, "agent must not be null"),
                AguiAdapterConfig.builder()
                        .defaultAgentId("ac-agentscope-v2")
                        .emitStateEvents(true)
                        .emitToolCallArgs(true)
                        .build());
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
        this.events = new AgentScopeAgUiEventMapper(Objects.requireNonNull(mapper, "mapper must not be null"));
    }

    public Mono<Void> execute(AgUiRunAgentInput input, RunScope scope) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        RunKey key = RunKey.from(scope);
        return adapter.run(toAgentScopeInput(input), AgentScopeRuntimeContexts.create(
                        scope,
                        event -> events.map(event).ifPresent(mapped -> sink.publish(scope, mapped))))
                .doOnSubscribe(subscription -> {
                    Subscription existing = active.putIfAbsent(key, subscription);
                    if (existing != null) {
                        subscription.cancel();
                        throw new IllegalStateException("AgentScope run is already active: " + scope.runId());
                    }
                })
                .doOnNext(event -> events.map(event).ifPresent(mapped -> sink.publish(scope, mapped)))
                .doFinally(signal -> active.remove(key))
                .then();
    }

    public boolean cancel(RunScope scope) {
        Subscription subscription = active.remove(RunKey.from(scope));
        if (subscription == null) return false;
        subscription.cancel();
        return true;
    }

    private RunAgentInput toAgentScopeInput(AgUiRunAgentInput input) {
        return new RunAgentInput(
                input.threadId(),
                input.runId(),
                input.messages().stream().map(this::toAgentScopeMessage).toList(),
                input.tools().stream().map(tool -> {
                    Map<String, Object> parameters = new LinkedHashMap<>();
                    parameters.put("type", tool.parameters().type());
                    parameters.put("properties", tool.parameters().properties());
                    parameters.put("required", tool.parameters().required());
                    return new AguiTool(tool.name(), tool.description(), parameters);
                }).toList(),
                input.context().stream()
                        .map(context -> new AguiContext(context.description(), context.value()))
                        .toList(),
                stringKeyedMap(input.state()),
                input.trustedForwardedProps(),
                input.resume().stream()
                        .map(resume -> new AguiResume(
                                resume.interruptId(),
                                resume.status().name().toLowerCase(java.util.Locale.ROOT),
                                resume.payload()))
                        .toList());
    }

    private AguiMessage toAgentScopeMessage(Message message) {
        List<AguiToolCall> toolCalls = message instanceof AssistantMessage assistant
                ? assistant.toolCalls().stream()
                        .map(call -> new AguiToolCall(call.id(), new AguiFunctionCall(
                                call.function().name(), call.function().arguments())))
                        .toList()
                : List.of();
        String toolCallId = message instanceof ToolMessage tool ? tool.toolCallId() : null;
        return AguiMessage.textMessage(
                message.id(), message.role().value(), message.content(), toolCalls, toolCallId);
    }

    private Map<String, Object> stringKeyedMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (key != null) result.put(String.valueOf(key), item);
        });
        return result;
    }

    private record RunKey(String tenantId, String userId, String threadId, String runId) {
        static RunKey from(RunScope scope) {
            return new RunKey(scope.tenantId(), scope.userId(), scope.threadId(), scope.runId());
        }
    }
}
