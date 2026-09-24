package com.ac.runtime.agentscope;

import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.InMemoryResultStore;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.tool.ToolIdentity;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentScopeIsolationContractTest {
    @Test
    void simultaneousClientsKeepRuntimeOwnershipAndEventQueuesSeparate() {
        Set<String> ownership = ConcurrentHashMap.newKeySet();
        IntStream.range(0, 200).parallel().forEach(index -> {
            var scope = new RunScope(
                    "tenant-" + index % 4, "user-" + index, "thread-" + index, "run-" + index, null);
            var context = AgentScopeRuntimeContexts.create(scope);
            var run = AgentScopeRuntimeContexts.require(context);
            run.emit("ui.surface.test", MapBuilder.value(index));
            ownership.add(run.scope().tenantId() + "/" + run.scope().userId() + "/"
                    + run.scope().threadId() + "/" + run.scope().runId());
            assertEquals(1, run.drainEvents().size());
        });
        assertEquals(200, ownership.size());
    }

    @Test
    void mismatchedRuntimeSessionIsRejected() {
        var first = AgentScopeRuntimeContexts.create(
                new RunScope("tenant", "user", "thread-a", "run-a", null));
        var second = AgentScopeRuntimeContexts.create(
                new RunScope("tenant", "user", "thread-b", "run-b", null));
        assertNotSame(first.get(AgentScopeRunContext.class), second.get(AgentScopeRunContext.class));
        first.put(AgentScopeRunContext.class, second.get(AgentScopeRunContext.class));
        assertThrows(IllegalStateException.class, () -> AgentScopeRuntimeContexts.require(first));
    }

    @Test
    void configuredEventConsumerReceivesToolEventsImmediately() {
        var delivered = new CopyOnWriteArrayList<String>();
        var context = AgentScopeRuntimeContexts.create(
                new RunScope("tenant", "user", "thread", "run", null),
                event -> delivered.add(event.getName()));
        var run = AgentScopeRuntimeContexts.require(context);

        run.emit("ui.surface.create", Map.of("surfaceId", "surface-1"));

        assertEquals(List.of("ui.surface.create"), delivered);
        assertEquals(0, run.drainEvents().size());
    }

    @Test
    void twoClientStoresCanUseTheSameOpaqueNameWithoutCrossReadingResults() {
        var sameReference = new ResultReference("result_same_name");
        var saaStore = new InMemoryResultStore(
                Duration.ofMinutes(5), 10, () -> sameReference, Clock.systemUTC());
        var agentScopeStore = new InMemoryResultStore(
                Duration.ofMinutes(5), 10, () -> sameReference, Clock.systemUTC());
        var saaScope = new RunScope("tenant", "saa-user", "thread", "run", "call");
        var agentScope = new RunScope("tenant", "agentscope-user", "thread", "run", "call");
        var tool = new ToolIdentity("knowledge", "lookup");
        saaStore.save(saaScope, tool, McpResult.success(
                Map.of("runtime", "saa"), null, PresentationHint.none(), null));
        agentScopeStore.save(agentScope, tool, McpResult.success(
                Map.of("runtime", "agentscope"), null, PresentationHint.none(), null));

        assertEquals(Map.of("runtime", "saa"), saaStore.get(
                sameReference, saaScope, AccessSubject.of("tenant", "saa-user")).result().data());
        assertEquals(Map.of("runtime", "agentscope"), agentScopeStore.get(
                sameReference, agentScope, AccessSubject.of("tenant", "agentscope-user")).result().data());
    }

    private static final class MapBuilder {
        static java.util.Map<String, Object> value(int index) { return java.util.Map.of("index", index); }
    }
}
