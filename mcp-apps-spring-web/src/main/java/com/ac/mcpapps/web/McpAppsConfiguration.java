package com.ac.mcpapps.web;

import com.ac.mcpapps.core.*;
import com.ac.agui.web.spi.AgUiSubjectResolver;
import com.ac.richui.core.result.ResultStore;
import com.ac.richui.core.tool.RichToolResultProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

/** Import in Host clients only. MCP Servers do not depend on this module. */
@Configuration
public class McpAppsConfiguration {
    @Bean public McpAppCatalog mcpAppCatalog() { return new McpAppCatalog(); }
    @Bean @ConditionalOnMissingBean(McpAppInstanceStore.class)
    public McpAppInstanceStore mcpAppInstances() { return new InMemoryMcpAppInstanceStore(Clock.systemUTC()); }
    @Bean @ConditionalOnMissingBean(McpAppPolicy.class)
    public McpAppPolicy mcpAppPolicy(@Value("${ac.mcp-apps.allowed-tools:}") String allowlist) {
        return McpAppPolicy.allowlist(new HashSet<>(Arrays.stream(allowlist.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList()));
    }
    @Bean public McpAppResourceService mcpAppResources(McpAppCatalog catalog, ObjectMapper mapper) {
        return new McpAppResourceService(catalog, mapper);
    }
    @Bean public McpAppService mcpAppService(McpAppCatalog catalog, McpAppResourceService resources,
            McpAppInstanceStore instances, McpAppPolicy policy, ResultStore results, RichToolResultProcessor processor,
            ObjectMapper mapper, @Value("${ac.deployment.distributed:false}") boolean distributed) {
        if (distributed && !instances.shared()) throw new IllegalStateException("Distributed deployment requires a shared McpAppInstanceStore");
        return new McpAppService(catalog, resources, instances, policy, results, processor, mapper, Clock.systemUTC());
    }
    @Bean public McpAppsController mcpAppsController(McpAppService service, AgUiSubjectResolver subjects) {
        return new McpAppsController(service, subjects);
    }
    @Bean public McpAppFollowups mcpAppFollowups(McpAppService service) { return new McpAppFollowups(service); }
    @Bean public McpAppResultPublisher mcpAppPublisher(McpAppService service, com.ac.richui.core.event.RuntimeEventSink events) {
        return new McpAppResultPublisher(service, events);
    }
}
