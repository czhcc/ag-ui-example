package com.ac.runtime.saa;

import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RuntimeEventSink;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** ReactAgent execution facade with scoped events, cancellation and HITL resume. */
public final class SaaAgentRuntime {
    private final ReactAgent agent;
    private final RuntimeEventSink events;
    private final Map<String, Sinks.Empty<Void>> cancellations = new ConcurrentHashMap<>();
    private final Map<String, PendingState> interruptions = new ConcurrentHashMap<>();
    private final Set<String> cancelledRuns = ConcurrentHashMap.newKeySet();

    public SaaAgentRuntime(ReactAgent agent, RuntimeEventSink events) {
        this.agent = Objects.requireNonNull(agent, "agent must not be null");
        this.events = Objects.requireNonNull(events, "events must not be null");
    }

    public Flux<String> stream(String message, RunScope scope) {
        return execute(message, scope, SaaRunMetadata.config(scope), false);
    }

    public Flux<String> resume(RunScope scope, List<HitlDecision> decisions) {
        return resume(scope, scope, decisions);
    }

    /** Resumes a checkpoint owned by {@code interruptedScope} as a new correlated run. */
    public Flux<String> resume(RunScope interruptedScope, RunScope outputScope, List<HitlDecision> decisions) {
        return Flux.defer(() -> {
            if (!interruptedScope.threadId().equals(outputScope.threadId())
                    || !interruptedScope.tenantId().equals(outputScope.tenantId())
                    || !interruptedScope.userId().equals(outputScope.userId())) {
                return Flux.error(new IllegalArgumentException("Resume scopes must have the same owner and thread"));
            }
            PendingState pending = interruptions.get(interruptedScope.runId());
            if (pending == null || !pending.scope().equals(interruptedScope)) {
                return Flux.error(new IllegalStateException("No interrupted run: " + interruptedScope.runId()));
            }
            InterruptionMetadata feedback = applyDecisions(pending.metadata(), decisions);
            if (!interruptions.remove(interruptedScope.runId(), pending)) {
                return Flux.error(new IllegalStateException("Interrupted run changed concurrently"));
            }
            RunnableConfig resumeConfig = RunnableConfig.builder(SaaRunMetadata.config(outputScope))
                    .addHumanFeedback(feedback)
                    .build();
            return execute("", outputScope, resumeConfig, true);
        });
    }

    public boolean cancel(RunScope scope) {
        Sinks.Empty<Void> signal = cancellations.get(scope.runId());
        if (signal == null) {
            return false;
        }
        cancelledRuns.add(scope.runId());
        events.publish(scope, StandardRuntimeEvent.of("run.cancelled"));
        return signal.tryEmitEmpty().isSuccess();
    }

    public PendingHumanInput pending(RunScope scope) {
        PendingState pending = interruptions.get(scope.runId());
        if (pending == null || !pending.scope().equals(scope)) {
            return null;
        }
        List<PendingHumanInput.ToolApproval> approvals = pending.metadata().toolFeedbacks().stream()
                .map(item -> new PendingHumanInput.ToolApproval(
                        item.getId(), item.getName(), item.getArguments(), item.getDescription()))
                .toList();
        return new PendingHumanInput(scope.runId(), approvals);
    }

    private Flux<String> execute(String message, RunScope scope, RunnableConfig config, boolean resumed) {
        return Flux.defer(() -> {
            Sinks.Empty<Void> cancellation = Sinks.empty();
            if (cancellations.putIfAbsent(scope.runId(), cancellation) != null) {
                return Flux.error(new IllegalStateException("Run is already active: " + scope.runId()));
            }
            events.publish(scope, new StandardRuntimeEvent(resumed ? "run.resumed" : "run.started", Map.of(
                    "threadId", scope.threadId(), "runId", scope.runId())));
            events.publish(scope, StandardRuntimeEvent.of("text.message.start"));
            Flux<NodeOutput> outputs;
            try {
                outputs = agent.stream(message, config);
            } catch (Exception exception) {
                cancellations.remove(scope.runId());
                return Flux.error(exception);
            }
            return outputs
                    .<String>handle((output, sink) -> mapOutput(scope, output, sink))
                    .takeUntilOther(cancellation.asMono())
                    .doOnError(error -> events.publish(scope, new StandardRuntimeEvent("run.error", Map.of(
                            "errorCode", error.getClass().getSimpleName()))))
                    .doOnComplete(() -> {
                        if (!interruptions.containsKey(scope.runId())
                                && !cancelledRuns.contains(scope.runId())) {
                            events.publish(scope, StandardRuntimeEvent.of("text.message.end"));
                            events.publish(scope, StandardRuntimeEvent.of("run.finished"));
                        }
                    })
                    .doFinally(signal -> {
                        cancellations.remove(scope.runId(), cancellation);
                        cancelledRuns.remove(scope.runId());
                    });
        });
    }

    private void mapOutput(RunScope scope, NodeOutput output, reactor.core.publisher.SynchronousSink<String> sink) {
        if (output instanceof InterruptionMetadata interruption) {
            interruptions.put(scope.runId(), new PendingState(scope, interruption));
            List<Map<String, Object>> approvals = interruption.toolFeedbacks().stream()
                    .map(item -> {
                        Map<String, Object> approval = new LinkedHashMap<>();
                        String toolCallId = item.getId() == null || item.getId().isBlank()
                                ? UUID.randomUUID().toString() : item.getId();
                        String toolName = item.getName() == null || item.getName().isBlank()
                                ? "unknown-tool" : item.getName();
                        approval.put("toolCallId", toolCallId);
                        approval.put("toolName", toolName);
                        if (item.getDescription() != null) approval.put("description", item.getDescription());
                        return Map.copyOf(approval);
                    })
                    .toList();
            events.publish(scope, new StandardRuntimeEvent("run.interrupted", Map.of("approvals", approvals)));
            return;
        }
        if (output instanceof StreamingOutput<?> streaming) {
            Message message = streaming.message();
            if ((streaming.getOutputType() == null
                    || streaming.getOutputType() == OutputType.AGENT_MODEL_STREAMING)
                    && message != null && message.getText() != null && !message.getText().isEmpty()) {
                String delta = message.getText();
                events.publish(scope, new StandardRuntimeEvent("text.message.content", Map.of("delta", delta)));
                sink.next(delta);
            }
        }
    }

    private InterruptionMetadata applyDecisions(InterruptionMetadata pending, List<HitlDecision> decisions) {
        Map<String, HitlDecision> byId = decisions == null ? Map.of() : decisions.stream()
                .collect(java.util.stream.Collectors.toMap(HitlDecision::toolCallId, decision -> decision));
        InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
                .nodeId(pending.node())
                .state(pending.state());
        for (InterruptionMetadata.ToolFeedback item : pending.toolFeedbacks()) {
            HitlDecision decision = byId.get(item.getId());
            if (decision == null) {
                throw new IllegalArgumentException("Missing HITL decision for tool call: " + item.getId());
            }
            var feedback = InterruptionMetadata.ToolFeedback.builder(item);
            switch (decision.action()) {
                case APPROVE -> feedback.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED);
                case REJECT -> feedback
                        .result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
                        .description(decision.reason());
                case EDIT -> feedback
                        .arguments(Objects.requireNonNull(decision.editedArguments(), "editedArguments is required"))
                        .result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED);
            }
            builder.addToolFeedback(feedback.build());
        }
        return builder.build();
    }

    private record PendingState(RunScope scope, InterruptionMetadata metadata) { }
}
