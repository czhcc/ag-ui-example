package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.agui.protocol.AgUiRunInputDecoder;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

/** 验证 AG-UI 控制器的运行、重连和取消生命周期。 */
class AgUiControllerLifecycleTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RunScope owner = new RunScope("tenant", "user", "thread", "run", null);

    @Test
    void responseCancellationPropagatesOwnerAndLeavesOneReplayableTerminal() throws Exception {
        var events = new AgUiEventStream(mapper, Duration.ofMinutes(5), 32, 128);
        var cancelled = new AtomicReference<RunScope>();
        var controller = controller(events, new AgUiRunHandler() {
            public Mono<Void> execute(AgUiRunAgentInput input, RunScope scope) { return Mono.never(); }
            public boolean cancel(RunScope scope) { cancelled.set(scope); return true; }
        });
        var body = controller.run(input(), null, request()).getBody();
        assertNotNull(body);
        StepVerifier.create(body).thenCancel().verify(Duration.ofSeconds(3));
        assertEquals(owner, cancelled.get());
        var replay = events.open(owner, null).collectList().block(Duration.ofSeconds(3));
        assertNotNull(replay);
        assertEquals(1, replay.stream().filter(e -> e.data().contains("\"type\":\"RUN_FINISHED\"")).count());
    }

    @Test
    void runtimeFailureProducesExactlyOneErrorTerminalWithoutExceptionText() throws Exception {
        var events = new AgUiEventStream(mapper, Duration.ofMinutes(5), 32, 128);
        var controller = controller(events, new AgUiRunHandler() {
            public Mono<Void> execute(AgUiRunAgentInput input, RunScope scope) {
                return Mono.error(new IllegalStateException("PRIVATE_FAILURE_CANARY"));
            }
            public boolean cancel(RunScope scope) { return true; }
        });
        var response = controller.run(input(), null, request()).getBody();
        assertNotNull(response);
        var frames = response.collectList().block(Duration.ofSeconds(3));
        assertNotNull(frames);
        assertEquals(1, frames.stream().filter(e -> e.data() != null && e.data().contains("RUN_ERROR")).count());
        assertFalse(frames.toString().contains("PRIVATE_FAILURE_CANARY"));
        assertFalse(frames.toString().contains("RUN_FINISHED"));
    }

    @Test
    void runtimeTimeoutProducesOneErrorTerminalAndClosesTheStream() throws Exception {
        var events = new AgUiEventStream(mapper, Duration.ofMinutes(5), 32, 128);
        var controller = controller(events, new AgUiRunHandler() {
            public Mono<Void> execute(AgUiRunAgentInput input, RunScope scope) { return Mono.never(); }
            public boolean cancel(RunScope scope) { return true; }
        });
        ReflectionTestUtils.setField(controller, "runTimeout", Duration.ofMillis(50));
        var response = controller.run(input(), null, request()).getBody();
        assertNotNull(response);

        var frames = response.collectList().block(Duration.ofSeconds(3));
        assertNotNull(frames);
        assertEquals(1, frames.stream().filter(e ->
                e.data() != null && e.data().contains("RUN_ERROR")).count());
        assertFalse(frames.toString().contains("RUN_FINISHED"));
    }

    private AgUiController controller(AgUiEventStream events, AgUiRunHandler handler) {
        return new AgUiController(new AgUiRunInputDecoder(mapper), events, handler,
                ignored -> AccessSubject.of("tenant", "user"));
    }
    private com.fasterxml.jackson.databind.JsonNode input() throws Exception {
        return mapper.readTree("""
                {"threadId":"thread","runId":"run","messages":[],"tools":[],"context":[],"resume":[]}
                """);
    }
    private MockServerHttpRequest request() { return MockServerHttpRequest.post("/api/agent").build(); }
}
