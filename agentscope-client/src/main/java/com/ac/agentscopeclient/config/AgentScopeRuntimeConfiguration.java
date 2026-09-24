package com.ac.agentscopeclient.config;

import com.ac.agentscopeclient.mcp.AgentScopeMcpRegistry;
import com.ac.richui.core.event.RuntimeEventSink;
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
import com.ac.runtime.agentscope.AgentScopeAgUiRuntime;
import com.ac.runtime.agentscope.AgentScopeRichMiddleware;
import com.ac.runtime.agentscope.AgentScopeUiRenderTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.HttpVersion;
import io.agentscope.core.model.transport.JdkHttpTransport;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import java.time.Duration;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@Configuration
public class AgentScopeRuntimeConfiguration {
    @Bean
    @ConditionalOnMissingBean(ResultStore.class)
    ResultStore resultStore(
            ObjectMapper mapper,
            @Value("${ac.agentscope.result-store.ttl:45m}") Duration ttl,
            @Value("${ac.agentscope.result-store.maximum-entries:10000}") int maximumEntries,
            @Value("${ac.agentscope.result-store.maximum-entries-per-tenant:1000}") int maximumEntriesPerTenant,
            @Value("${ac.agentscope.result-store.maximum-bytes-per-result:2097152}") long maximumBytesPerResult,
            @Value("${ac.agentscope.result-store.maximum-bytes-per-tenant:268435456}") long maximumBytesPerTenant) {
        return new InMemoryResultStore(new ResultStoreLimits(ttl, maximumEntries,
                maximumEntriesPerTenant, maximumBytesPerResult, maximumBytesPerTenant),
                new UuidResultReferenceGenerator(), Clock.systemUTC(),
                (result, stopAfter) -> jsonBytes(mapper, result, stopAfter));
    }

    @Bean ObservationBuilder observationBuilder() { return new DefaultObservationBuilder(); }
    @Bean PresentationService presentationService(ResultStore store) { return new DefaultPresentationService(store); }
    @Bean McpCallToolResultAdapter mcpCallToolResultAdapter(ObjectMapper mapper) {
        return new McpCallToolResultAdapter(mapper);
    }
    @Bean McpResultDecoder mcpResultDecoder(ObjectMapper mapper) { return new DefaultMcpResultDecoder(mapper); }
    @Bean RichToolResultProcessor richToolResultProcessor(
            McpResultDecoder decoder, ResultStore store, ObservationBuilder observations) {
        return new DefaultRichToolResultProcessor(decoder, store, observations);
    }
    @Bean AgentScopeRichMiddleware agentScopeRichMiddleware() { return new AgentScopeRichMiddleware(); }
    @Bean
    @ConditionalOnMissingBean(AgentStateStore.class)
    AgentStateStore agentStateStore() { return new InMemoryAgentStateStore(); }
    @Bean AgentScopeDistributedStateGuard agentScopeDistributedStateGuard(
            AgentStateStore stateStore,
            @Value("${ac.deployment.distributed:false}") boolean distributed) {
        return new AgentScopeDistributedStateGuard(distributed, stateStore);
    }
    @Bean AgentScopeUiRenderTool agentScopeUiRenderTool(PresentationService presentations) {
        return new AgentScopeUiRenderTool(presentations);
    }

    @Bean
    Toolkit agentScopeToolkit(AgentScopeMcpRegistry mcp, AgentScopeUiRenderTool uiRender) {
        Toolkit toolkit = new Toolkit();
        mcp.tools().forEach(toolkit::registerAgentTool);
        toolkit.registerAgentTool(uiRender);
        return toolkit;
    }

    @Bean
    Model agentScopeModel(
            @Value("${ac.agentscope.model.api-key:}") String apiKey,
            @Value("${ac.agentscope.model.name}") String modelName,
            @Value("${ac.agentscope.model.base-url}") String baseUrl,
            @Value("${ac.agentscope.model.endpoint-path:/chat/completions}") String endpointPath,
            @Value("${ac.agentscope.model.http-version:HTTP_1_1}") HttpVersion httpVersion) {
        JdkHttpTransport transport = JdkHttpTransport.builder()
                .config(HttpTransportConfig.builder().httpVersion(httpVersion).build())
                .build();
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .endpointPath(endpointPath)
                .httpTransport(transport)
                .stream(true)
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Agent.class)
    ReActAgent agentScopeAgent(
            Model model, Toolkit toolkit, AgentScopeRichMiddleware middleware,
            ObjectProvider<AgentStateStore> stateStore,
            @Value("${ac.agentscope.agent.name:ac-rich-agent}") String name,
            @Value("${ac.agentscope.agent.system-prompt}") String systemPrompt) {
        ReActAgent.Builder builder = ReActAgent.builder()
                .name(name)
                .sysPrompt(systemPrompt)
                .model(model)
                .toolkit(toolkit)
                .middleware(middleware);
        AgentStateStore configuredStore = stateStore.getIfAvailable();
        if (configuredStore != null) builder.stateStore(configuredStore);
        return builder.build();
    }

    @Bean
    AgentScopeAgUiRuntime agentScopeAgUiRuntime(
            Agent agent, RuntimeEventSink events, ObjectMapper mapper) {
        return new AgentScopeAgUiRuntime(agent, events, mapper);
    }

    private long jsonBytes(ObjectMapper mapper, Object value, long stopAfter) {
        try {
            return mapper.writeValueAsBytes(value).length;
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return stopAfter == Long.MAX_VALUE ? Long.MAX_VALUE : stopAfter + 1;
        }
    }
}
