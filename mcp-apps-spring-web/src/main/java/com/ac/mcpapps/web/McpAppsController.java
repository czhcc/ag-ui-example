package com.ac.mcpapps.web;

import com.ac.mcpapps.core.*;
import com.ac.agui.web.spi.AgUiSubjectResolver;
import com.ac.richui.core.context.AccessSubject;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.Callable;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Blocking MCP I/O runs on boundedElastic, never on the WebFlux event loop. */
@RestController
@RequestMapping("/api/mcp-apps/instances")
public final class McpAppsController {
    private final McpAppService apps;
    private final AgUiSubjectResolver subjects;
    public McpAppsController(McpAppService apps, AgUiSubjectResolver subjects) { this.apps = apps; this.subjects = subjects; }
    @GetMapping("/{id}") public Mono<ResponseEntity<?>> snapshot(@PathVariable("id") String id, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> apps.snapshot(id, subject));
    }
    @GetMapping("/{id}/resource") public Mono<ResponseEntity<?>> resource(@PathVariable("id") String id, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> apps.resource(id, subject));
    }
    @GetMapping("/{id}/initial-result") public Mono<ResponseEntity<?>> initial(@PathVariable("id") String id, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> apps.initialResult(id, subject));
    }
    @GetMapping("/{id}/latest-result") public Mono<ResponseEntity<?>> latest(@PathVariable("id") String id, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> apps.latestResult(id, subject));
    }
    public record ToolCall(String clientRequestId, long expectedRevision, String toolName, Map<String, Object> arguments) {}
    @PostMapping("/{id}/tool-calls") public Mono<ResponseEntity<?>> invoke(@PathVariable("id") String id, @RequestBody ToolCall call, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> apps.invoke(id, subject, call.clientRequestId(),
                call.expectedRevision(), call.toolName(), call.arguments() == null ? Map.of() : call.arguments()));
    }
    @PostMapping("/{id}/context") public Mono<ResponseEntity<?>> context(@PathVariable("id") String id, @RequestBody Map<String, Object> context, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> { apps.context(id, subject, context); return Map.of(); });
    }
    public record Followup(String question) {}
    @PostMapping("/{id}/followups") public Mono<ResponseEntity<?>> followup(@PathVariable("id") String id, @RequestBody Followup followup, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> apps.followup(id, subject, followup.question()));
    }
    @DeleteMapping("/{id}") public Mono<ResponseEntity<?>> close(@PathVariable("id") String id, ServerHttpRequest request) {
        var subject = subjects.resolve(request); return response(() -> { apps.close(id, subject); return Map.of(); });
    }
    private Mono<ResponseEntity<?>> response(Callable<Object> action) {
        return Mono.<ResponseEntity<?>>fromCallable(() -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(action.call()))
                .subscribeOn(Schedulers.boundedElastic()).onErrorResume(McpAppException.class, e -> {
                    int status = switch (e.code()) {
                        case "APP_ACCESS_DENIED", "APP_NOT_ALLOWED", "APP_TOOL_DENIED" -> 403;
                        case "APP_NOT_FOUND" -> 404;
                        case "APP_EXPIRED", "APP_CLOSED" -> 410;
                        case "APP_REVISION_CONFLICT", "APP_VERSION_MISMATCH" -> 409;
                        case "APP_RATE_LIMITED", "APP_QUOTA_EXCEEDED" -> 429;
                        default -> 400;
                    };
                    return Mono.just(ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("code", e.code())));
                });
    }
}
