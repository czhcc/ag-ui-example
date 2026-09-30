package com.ac.runtime.agentscope;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.observation.DefaultObservationBuilder;
import com.ac.richui.core.presentation.DefaultPresentationService;
import com.ac.richui.core.result.InMemoryResultStore;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.tool.ProcessedToolResult;
import com.ac.richui.core.tool.RawToolResult;
import com.ac.richui.core.tool.ToolIdentity;
import com.ac.richui.mcp.DefaultMcpResultDecoder;
import com.ac.richui.mcp.DefaultRichToolResultProcessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Golden semantic contract consumed by both the SAA and AgentScope runtime boundaries. */
class DualRuntimeRichContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void saaAndAgentScopeProduceEquivalentPortableContracts() throws Exception {
        Map<String, Object> fixture;
        try (InputStream input = getClass().getResourceAsStream("/contracts/rich-table.json")) {
            fixture = mapper.readValue(input, new TypeReference<>() { });
        }

        Outcome saa = process("contract-ref", fixture);
        Outcome agentScope = process("contract-ref", fixture);

        assertEquals(saa.processed().observation(), agentScope.processed().observation());
        assertEquals(saa.processed().resultReference(), agentScope.processed().resultReference());
        assertEquals(saa.surface().profile(), agentScope.surface().profile());
        assertEquals(saa.surface().profileVersion(), agentScope.surface().profileVersion());
        assertEquals(saa.surface().dataRef(), agentScope.surface().dataRef());
        assertEquals(saa.surface().components(), agentScope.surface().components());
    }

    private Outcome process(String reference, Map<String, Object> fixture) {
        var store = new InMemoryResultStore(
                Duration.ofMinutes(5), 10, () -> new ResultReference(reference), Clock.systemUTC());
        var processor = new DefaultRichToolResultProcessor(
                new DefaultMcpResultDecoder(mapper), store, new DefaultObservationBuilder());
        var scope = new RunScope("tenant", "user", "thread", "run", "call");
        ProcessedToolResult processed = processor.process(
                scope, new ToolIdentity("knowledge", "city_stats"),
                new RawToolResult(false, fixture, "ignored native text"));
        SurfaceSpec surface = new DefaultPresentationService(store).render(
                scope, AccessSubject.of("tenant", "user"), processed.resultReference(), "city-table");
        return new Outcome(processed, surface);
    }

    private record Outcome(ProcessedToolResult processed, SurfaceSpec surface) { }
}
