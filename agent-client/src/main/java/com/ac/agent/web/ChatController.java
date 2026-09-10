package com.ac.agent.web;

import com.ac.agent.agent.AgentContext;
import com.ac.agent.agent.AgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final AgentService agentService;
    private final com.ac.agent.streaming.AgentEventBus eventBus;

    public ChatController(AgentService agentService, com.ac.agent.streaming.AgentEventBus eventBus) {
        this.agentService = agentService;
        this.eventBus = eventBus;
    }

    /**
     * Backwards-compatible raw text stream.
     */
    @PostMapping(value = "/runs", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> run(@Valid @RequestBody ChatRequest request) {
        AgentContext context = createContext(request.conversationId());
        return agentService.stream(request.message(), context);
    }

    /**
     * Browser-facing structured stream. Every response is an SSE event so clients can
     * distinguish content, completion and failure without parsing model output.
     */
    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> messages(@Valid @RequestBody ChatRequest request) {
        AgentContext context = createContext(request.conversationId());

        Flux<ServerSentEvent<ChatStreamEvent>> content = agentService.stream(request.message(), context)
                .map(delta -> event("delta", new ChatStreamEvent(
                        "delta", context.conversationId(), context.runId(), delta)));

        Flux<ServerSentEvent<UiStreamEvent>> uiEvents = eventBus.events()
                .filter(e -> e.type() == com.ac.agent.streaming.AgentEventType.UI_CREATE)
                .map(e -> {
                    var surface = ((com.ac.agent.streaming.event.UiCreateEvent) e).surface();
                    return ServerSentEvent.<UiStreamEvent>builder(new UiStreamEvent(
                                    context.conversationId(), context.runId(),
                                    surface.surfaceId(), surface.dataRef(), surface.components()))
                            .event("ui")
                            .build();
                });

        ServerSentEvent<Object> doneEvent = ServerSentEvent.<Object>builder(new ChatStreamEvent(
                "done", context.conversationId(), context.runId(), null)).event("done").build();

        Flux<ServerSentEvent<Object>> textStream = content.map(se -> ServerSentEvent.<Object>builder(se.data()).event(se.event()).build());
        Flux<ServerSentEvent<Object>> uiStream = uiEvents.map(se -> ServerSentEvent.<Object>builder(se.data()).event(se.event()).build());

        reactor.core.publisher.Mono<Void> runFinished = eventBus.events()
                .filter(e -> e.type() == com.ac.agent.streaming.AgentEventType.RUN_FINISHED
                        && context.runId().equals(((com.ac.agent.streaming.event.RunFinishedEvent) e).runId()))
                .next()
                .then();
        return textStream.mergeWith(uiStream.takeUntilOther(runFinished))
                .concatWithValues(doneEvent)
                .onErrorResume(error -> Flux.just(ServerSentEvent.<Object>builder(new ChatStreamEvent(
                        "error",
                        context.conversationId(),
                        context.runId(),
                        "智能体暂时无法响应，请稍后重试。")).event("error").build()));
    }

    private ServerSentEvent<ChatStreamEvent> event(String name, ChatStreamEvent data) {
        return ServerSentEvent.<ChatStreamEvent>builder(data).event(name).build();
    }

    @SuppressWarnings("unused")
    private ServerSentEvent<UiStreamEvent> event(String name, UiStreamEvent data) {
        return ServerSentEvent.<UiStreamEvent>builder(data).event(name).build();
    }

    private AgentContext createContext(String requestedConversationId) {
        String conversationId = requestedConversationId == null || requestedConversationId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedConversationId;
        return new AgentContext(conversationId, UUID.randomUUID().toString(), null);
    }

    public record ChatRequest(
            @Size(max = 128) String conversationId,
            @NotBlank @Size(max = 8000) String message) {
    }

    public record ChatStreamEvent(
            String type,
            String conversationId,
            String runId,
            String content) {
    }

    public record UiStreamEvent(
            String conversationId,
            String runId,
            String surfaceId,
            String dataRef,
            java.util.List<?> components) {
    }
}
