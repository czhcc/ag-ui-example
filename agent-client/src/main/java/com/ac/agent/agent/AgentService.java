package com.ac.agent.agent;

import com.ac.agent.streaming.AgentEventBus;
import com.ac.agent.streaming.event.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class AgentService {
    private final AgentFactory factory;
    private final AgentEventBus events;

    public AgentService(AgentFactory factory, AgentEventBus events) {
        this.factory = factory;
        this.events = events;
    }

    public Flux<String> stream(String message, AgentContext context) {
        String messageId = UUID.randomUUID().toString();
        return Mono.fromCallable(() -> {
                    events.emit(new TextStartEvent(messageId));
                    return factory.create();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(client -> client.prompt()
                        .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, context.conversationId()))
                        .user(message)
                        .stream()
                        .content())
                .doOnNext(delta -> events.emit(new TextDeltaEvent(messageId, delta)))
                .doOnError(error -> events.emit(new ErrorEvent(
                        "CHAT_STREAM_ERROR", "智能体响应失败，请稍后重试。")))
                .doOnComplete(() -> {
                    events.emit(new TextEndEvent(messageId));
                    events.emit(new RunFinishedEvent(context.runId()));
                });
    }
}
