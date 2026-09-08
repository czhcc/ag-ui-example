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

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
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
    public Flux<ServerSentEvent<ChatStreamEvent>> messages(@Valid @RequestBody ChatRequest request) {
        AgentContext context = createContext(request.conversationId());

        Flux<ServerSentEvent<ChatStreamEvent>> content = agentService.stream(request.message(), context)
                .map(delta -> event("delta", new ChatStreamEvent(
                        "delta", context.conversationId(), context.runId(), delta)));

        ServerSentEvent<ChatStreamEvent> completed = event("done", new ChatStreamEvent(
                "done", context.conversationId(), context.runId(), null));

        return content.concatWithValues(completed)
                .onErrorResume(error -> Flux.just(event("error", new ChatStreamEvent(
                        "error",
                        context.conversationId(),
                        context.runId(),
                        "智能体暂时无法响应，请稍后重试。"))));
    }

    private AgentContext createContext(String requestedConversationId) {
        String conversationId = requestedConversationId == null || requestedConversationId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedConversationId;
        return new AgentContext(conversationId, UUID.randomUUID().toString(), null);
    }

    private ServerSentEvent<ChatStreamEvent> event(String name, ChatStreamEvent data) {
        return ServerSentEvent.<ChatStreamEvent>builder(data)
                .event(name)
                .build();
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
}
