package com.ac.agent.streaming;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class SseController {
    private final AgentEventBus eventBus;
    public SseController(AgentEventBus eventBus) { this.eventBus = eventBus; }
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentEvent>> stream() {
        return eventBus.events().map(event -> ServerSentEvent.builder(event).event(event.type().name()).build());
    }
}
