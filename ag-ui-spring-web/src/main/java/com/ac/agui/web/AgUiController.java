package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.agui.protocol.AgUiRunInputDecoder;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agent")
public final class AgUiController {
    private final AgUiRunInputDecoder decoder;
    private final AgUiRunStateStore events;
    private final AgUiRunHandler handler;
    private final AgUiSubjectResolver subjects;
    @Value("${ac.ag-ui.run-timeout:2m}")
    private Duration runTimeout = Duration.ofMinutes(2);

    public AgUiController(AgUiRunInputDecoder decoder, AgUiRunStateStore events,
                          AgUiRunHandler handler, AgUiSubjectResolver subjects) {
        this.decoder = decoder;
        this.events = events;
        this.handler = handler;
        this.subjects = subjects;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> run(
            @RequestBody JsonNode body,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            ServerHttpRequest request) {
        AgUiRunAgentInput input = decoder.decode(body);
        AccessSubject subject = subjects.resolve(request);
        RunScope scope = new RunScope(subject.tenantId(), subject.userId(),
                input.threadId(), input.runId(), null);
        AgUiRunStateStore.Registration registration = events.register(scope, input, fingerprint(body));

        Flux<ServerSentEvent<String>> frames = events.open(scope, lastEventId)
                .map(event -> ServerSentEvent.<String>builder(event.data()).id(event.id()).build());
        Flux<ServerSentEvent<String>> shared = frames.publish().autoConnect(2);
        Flux<ServerSentEvent<String>> keepAlive = Flux.interval(Duration.ofSeconds(15))
                .map(ignored -> ServerSentEvent.<String>builder().comment("keep-alive").build())
                .takeUntilOther(shared.ignoreElements());
        Flux<ServerSentEvent<String>> execution = registration.execute()
                ? Mono.defer(() -> handler.execute(input, scope))
                    .timeout(runTimeout)
                    .doOnSuccess(ignored -> events.finishIfMissing(scope, "completed"))
                    .doOnError(error -> events.failIfMissing(scope, error.getClass().getSimpleName()))
                    .onErrorResume(error -> Mono.empty())
                    .thenMany(Flux.<ServerSentEvent<String>>empty())
                : Flux.<ServerSentEvent<String>>empty();
        Flux<ServerSentEvent<String>> response = Flux.merge(shared, keepAlive, execution);
        if (registration.execute()) {
            response = response.doOnCancel(() -> {
                handler.cancel(scope);
                events.finishIfMissing(scope, "cancelled");
            });
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(response);
    }

    @GetMapping("/capabilities")
    public Map<String, Object> capabilities() {
        return Map.of(
                "protocol", "AG-UI",
                "requestModel", "RunAgentInput",
                "events", List.of(
                        "RUN_STARTED", "RUN_FINISHED", "RUN_ERROR",
                        "TEXT_MESSAGE_START", "TEXT_MESSAGE_CONTENT", "TEXT_MESSAGE_END",
                        "TOOL_CALL_START", "TOOL_CALL_ARGS", "TOOL_CALL_END", "TOOL_CALL_RESULT",
                        "STATE_SNAPSHOT", "CUSTOM"),
                "stateSnapshot", true,
                "stateDelta", false,
                "frontendToolExecution", false,
                "reconnect", Map.of("lastEventId", true, "idempotentRunId", true));
    }

    @DeleteMapping("/threads/{threadId}/runs/{runId}")
    public ResponseEntity<Void> cancel(@PathVariable("threadId") String threadId,
                                       @PathVariable("runId") String runId,
                                       ServerHttpRequest request) {
        AccessSubject subject = subjects.resolve(request);
        RunScope scope = new RunScope(subject.tenantId(), subject.userId(), threadId, runId, null);
        if (!handler.cancel(scope)) return ResponseEntity.notFound().build();
        events.finishIfMissing(scope, "cancelled");
        return ResponseEntity.accepted().build();
    }

    private String fingerprint(JsonNode body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(body.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
