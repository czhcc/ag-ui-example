package com.ac.agui.web.stream;

import com.ac.agui.protocol.AgUiEventEncoder;
import com.ac.agui.protocol.AgUiEventTranslator;
import com.ac.agui.protocol.AgUiProtocolException;
import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RichRuntimeEvent;
import com.agui.community.core.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** 按访问主体、线程和运行隔离的进程内事件流，支持幂等注册与事件重放。 */
public final class AgUiEventStream implements AgUiRunStateStore {
    private final Map<RunKey, RunState> runs = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final AgUiEventEncoder encoder;
    private final Duration retention;
    private final int replayLimit;
    private final int maximumRuns;

    /** 配置事件保留时间、重放条数和最大运行数。 */
    public AgUiEventStream(ObjectMapper mapper,
                           @Value("${ac.ag-ui.retention:30m}") Duration retention,
                           @Value("${ac.ag-ui.replay-limit:512}") int replayLimit,
                           @Value("${ac.ag-ui.maximum-runs:10000}") int maximumRuns) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.encoder = new AgUiEventEncoder(mapper);
        this.retention = Objects.requireNonNull(retention, "retention must not be null");
        this.replayLimit = Math.max(16, replayLimit);
        this.maximumRuns = Math.max(128, maximumRuns);
    }

    /** 以请求指纹幂等注册运行，仅首次注册获得执行权。 */
    @Override
    public Registration register(RunScope scope, AgUiRunAgentInput input, String fingerprint) {
        prune();
        RunKey key = RunKey.from(scope);
        RunState registered = runs.get(key);
        if (registered != null) {
            if (!registered.fingerprint.equals(fingerprint)) {
                throw new AgUiProtocolException("RUN_ID_CONFLICT",
                        "runId is already registered with a different request");
            }
            registered.touch();
            return new Registration(scope, false);
        }
        if (runs.size() >= maximumRuns) {
            throw new AgUiProtocolException("RUN_CAPACITY_EXCEEDED", "AG-UI run capacity exceeded");
        }
        RunState created = new RunState(scope, input, fingerprint, mapper, replayLimit);
        RunState existing = runs.putIfAbsent(key, created);
        RunState state = existing == null ? created : existing;
        if (!state.fingerprint.equals(fingerprint)) {
            throw new AgUiProtocolException("RUN_ID_CONFLICT",
                    "runId is already registered with a different request");
        }
        state.touch();
        boolean execute = existing == null && state.executionClaimed.compareAndSet(false, true);
        return new Registration(scope, execute);
    }

    /** 从指定事件序号后重放，并订阅后续事件。 */
    @Override
    public Flux<EncodedEvent> open(RunScope scope, String lastEventId) {
        RunState state = require(scope);
        long after = parseSequence(scope.runId(), lastEventId);
        long firstRetained = Math.max(1, state.sequence.get() - replayLimit + 1);
        if (after > 0 && after < firstRetained - 1) {
            throw new AgUiProtocolException("REPLAY_WINDOW_EXPIRED",
                    "Last-Event-ID is older than the retained replay window");
        }
        state.touch();
        return state.events.asFlux().filter(event -> event.sequence() > after);
    }

    /** 转换并发布指定运行范围内的内部事件。 */
    @Override
    public void publish(RunScope scope, RichRuntimeEvent event) {
        RunState state = runs.get(RunKey.from(scope));
        if (state == null) return;
        synchronized (state) {
            for (Event translated : state.translator.translate(event)) emit(state, translated);
            if (state.translator.terminal()) state.events.tryEmitComplete();
            state.touch();
        }
    }

    /** 尚无终态时发布运行完成事件。 */
    @Override
    public void finishIfMissing(RunScope scope, String status) {
        RunState state = runs.get(RunKey.from(scope));
        if (state == null) return;
        synchronized (state) {
            for (Event event : state.translator.finishIfMissing(status)) emit(state, event);
            state.events.tryEmitComplete();
            state.touch();
        }
    }

    /** 尚无终态时发布运行错误事件。 */
    @Override
    public void failIfMissing(RunScope scope, String code) {
        RunState state = runs.get(RunKey.from(scope));
        if (state == null) return;
        synchronized (state) {
            for (Event event : state.translator.failIfMissing(code)) emit(state, event);
            state.events.tryEmitComplete();
            state.touch();
        }
    }

    private void emit(RunState state, Event event) {
        long sequence = state.sequence.incrementAndGet();
        state.events.tryEmitNext(new EncodedEvent(
                state.scope.runId() + ":" + sequence, sequence, encoder.encode(event)));
    }

    private RunState require(RunScope scope) {
        RunState state = runs.get(RunKey.from(scope));
        if (state == null) throw new AgUiProtocolException("RUN_NOT_FOUND", "Unknown runId");
        return state;
    }

    private long parseSequence(String runId, String eventId) {
        if (eventId == null || eventId.isBlank()) return 0;
        String prefix = runId + ":";
        if (!eventId.startsWith(prefix)) {
            throw new AgUiProtocolException("INVALID_LAST_EVENT_ID", "Last-Event-ID does not belong to this run");
        }
        try {
            long sequence = Long.parseLong(eventId.substring(prefix.length()));
            if (sequence < 0) throw new NumberFormatException("negative sequence");
            return sequence;
        } catch (NumberFormatException exception) {
            throw new AgUiProtocolException("INVALID_LAST_EVENT_ID", "Last-Event-ID has an invalid sequence");
        }
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(retention);
        runs.entrySet().removeIf(entry -> entry.getValue().updatedAt.isBefore(cutoff)
                && entry.getValue().translator.terminal());
    }

    /** 单次运行的事件序号、转换器和重放缓存。 */
    private static final class RunState {
        private final RunScope scope;
        private final String fingerprint;
        private final AgUiEventTranslator translator;
        private final Sinks.Many<EncodedEvent> events;
        private final AtomicLong sequence = new AtomicLong();
        private final java.util.concurrent.atomic.AtomicBoolean executionClaimed =
                new java.util.concurrent.atomic.AtomicBoolean();
        private volatile Instant updatedAt = Instant.now();

        private RunState(RunScope scope, AgUiRunAgentInput input, String fingerprint,
                         ObjectMapper mapper, int replayLimit) {
            this.scope = scope;
            this.fingerprint = fingerprint;
            this.translator = new AgUiEventTranslator(scope, input, mapper);
            this.events = Sinks.many().replay().limit(replayLimit);
        }

        private void touch() { updatedAt = Instant.now(); }
    }

    /** 按归属主体、线程和运行标识隔离的存储键。 */
    private record RunKey(String tenantId, String userId, String threadId, String runId) {
        static RunKey from(RunScope scope) {
            return new RunKey(scope.tenantId(), scope.userId(), scope.threadId(), scope.runId());
        }
    }
}
