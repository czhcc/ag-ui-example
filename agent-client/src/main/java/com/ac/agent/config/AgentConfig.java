package com.ac.agent.config;

import com.ac.richui.core.observation.DefaultObservationBuilder;
import com.ac.richui.core.observation.ObservationBuilder;
import com.ac.richui.core.presentation.DefaultPresentationService;
import com.ac.richui.core.presentation.PresentationService;
import com.ac.richui.core.result.InMemoryResultStore;
import com.ac.richui.core.result.ResultStore;
import com.ac.richui.core.result.ResultStoreLimits;
import com.ac.richui.core.result.UuidResultReferenceGenerator;
import com.ac.richui.core.tool.RichToolResultProcessor;
import com.ac.richui.mcp.DefaultMcpResultDecoder;
import com.ac.richui.mcp.DefaultRichToolResultProcessor;
import com.ac.richui.mcp.McpCallToolResultAdapter;
import com.ac.richui.mcp.McpResultDecoder;
import com.ac.runtime.saa.SaaToolEventInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {
    @Bean
    @ConditionalOnMissingBean(ResultStore.class)
    public ResultStore resultStore(
            ObjectMapper mapper,
            @Value("${ac.result-store.ttl:45m}") Duration ttl,
            @Value("${ac.result-store.maximum-entries:10000}") int maximumEntries,
            @Value("${ac.result-store.maximum-entries-per-tenant:1000}") int maximumEntriesPerTenant,
            @Value("${ac.result-store.maximum-bytes-per-result:2097152}") long maximumBytesPerResult,
            @Value("${ac.result-store.maximum-bytes-per-tenant:268435456}") long maximumBytesPerTenant) {
        return new InMemoryResultStore(new ResultStoreLimits(ttl, maximumEntries,
                maximumEntriesPerTenant, maximumBytesPerResult, maximumBytesPerTenant),
                new UuidResultReferenceGenerator(), Clock.systemUTC(),
                (result, stopAfter) -> jsonBytes(mapper, result, stopAfter));
    }

    @Bean
    public ObservationBuilder observationBuilder() {
        return new DefaultObservationBuilder();
    }

    @Bean
    public PresentationService presentationService(ResultStore resultStore) {
        return new DefaultPresentationService(resultStore);
    }

    @Bean
    public McpCallToolResultAdapter mcpCallToolResultAdapter(ObjectMapper mapper) {
        return new McpCallToolResultAdapter(mapper);
    }

    @Bean
    public McpResultDecoder mcpResultDecoder(ObjectMapper mapper) {
        return new DefaultMcpResultDecoder(mapper);
    }

    @Bean
    public RichToolResultProcessor richToolResultProcessor(
            McpResultDecoder decoder, ResultStore store, ObservationBuilder observations) {
        return new DefaultRichToolResultProcessor(decoder, store, observations);
    }

    @Bean
    @ConditionalOnMissingBean(BaseCheckpointSaver.class)
    public BaseCheckpointSaver checkpointSaver() {
        return new MemorySaver();
    }

    @Bean
    public SaaDistributedStateGuard saaDistributedStateGuard(
            BaseCheckpointSaver saver,
            @Value("${ac.deployment.distributed:false}") boolean distributed) {
        return new SaaDistributedStateGuard(distributed, saver);
    }

    @Bean
    public SaaToolEventInterceptor saaToolEventInterceptor(com.ac.richui.core.event.RuntimeEventSink events) {
        return new SaaToolEventInterceptor(events);
    }

    private long jsonBytes(ObjectMapper mapper, Object value, long stopAfter) {
        try {
            return mapper.writeValueAsBytes(value).length;
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return stopAfter == Long.MAX_VALUE ? Long.MAX_VALUE : stopAfter + 1;
        }
    }
}
