package com.ac.agui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.agui.community.core.message.UserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/** 验证事件流的注册幂等性、序号和重放行为。 */
class AgUiEventStreamTest {
    @Test
    void isolatesRunsEnforcesIdempotencyAndReplaysAfterEventId() {
        AgUiEventStream stream = new AgUiEventStream(new ObjectMapper(), Duration.ofMinutes(5), 32, 128);
        RunScope scope = new RunScope("t", "u", "thread", "run", null);
        AgUiRunAgentInput input = new AgUiRunAgentInput("thread", "run", null, null,
                List.of(new UserMessage("m", "hello")), List.of(), List.of(), null, List.of());
        assertTrue(stream.register(scope, input, "same").execute());
        assertFalse(stream.register(scope, input, "same").execute());
        stream.publish(scope, new StandardRuntimeEvent("run.started", Map.of()));
        stream.publish(scope, StandardRuntimeEvent.of("run.finished"));
        stream.publish(scope, new StandardRuntimeEvent("run.error", Map.of("errorCode", "LATE_ERROR")));

        StepVerifier.create(stream.open(scope, "run:1"))
                .assertNext(event -> assertEquals("run:2", event.id()))
                .verifyComplete();
    }

    @Test
    void concurrentUsersThreadsAndRunsNeverShareReplayStreams() {
        AgUiEventStream stream = new AgUiEventStream(new ObjectMapper(), Duration.ofMinutes(5), 32, 256);
        List<RunScope> scopes = new ArrayList<>();
        IntStream.range(0, 40).forEach(index -> scopes.add(
                new RunScope("tenant", "same-user", "same-thread", "run-" + index, null)));
        IntStream.range(0, 40).forEach(index -> scopes.add(
                new RunScope("tenant", "user-" + index, "same-thread", "same-run", null)));
        IntStream.range(0, 40).forEach(index -> scopes.add(
                new RunScope("tenant", "same-user", "thread-" + index, "shared-run", null)));

        scopes.parallelStream().forEach(scope -> {
            AgUiRunAgentInput input = new AgUiRunAgentInput(
                    scope.threadId(), scope.runId(), null, null,
                    List.of(), List.of(), List.of(), null, List.of());
            assertTrue(stream.register(scope, input, scope.toString()).execute());
            stream.publish(scope, new StandardRuntimeEvent("run.started", Map.of()));
            stream.publish(scope, StandardRuntimeEvent.of("run.finished"));
        });

        scopes.parallelStream().forEach(scope -> {
            List<AgUiRunStateStore.EncodedEvent> replay = stream.open(scope, null)
                    .collectList().block(Duration.ofSeconds(2));
            assertEquals(2, replay.size());
            assertEquals(scope.runId() + ":1", replay.get(0).id());
            assertTrue(replay.get(0).data().contains("\"threadId\":\"" + scope.threadId() + "\""));
            assertTrue(replay.get(0).data().contains("\"runId\":\"" + scope.runId() + "\""));
        });
    }
}
