package com.ac.mcp.server;

import com.ac.mcp.server.service.PhaseTwoFixtureService;
import com.ac.mcp.server.tool.phase2.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class DefaultToolRegistrationTest {
    @Test void allToolsAreRegisteredWithoutAnActiveProfile() {
        try (var context = new AnnotationConfigApplicationContext()) {
            assertEquals(0, context.getEnvironment().getActiveProfiles().length);
            context.scan("com.ac.mcp.server.service", "com.ac.mcp.server.tool");
            context.refresh();

            assertNotNull(context.getBean(PhaseTwoFixtureService.class));
            for (var type : new Class<?>[]{ActivityMetricsTool.class, KnowledgeOverviewTool.class,
                    EntityCardsTool.class, RelationTreeTool.class, ActivityHeatmapTool.class,
                    RelationshipPathTool.class, EvidenceChainTool.class}) {
                assertNotNull(context.getBean(type));
            }
            long toolCount = Arrays.stream(context.getBeanDefinitionNames())
                    .map(context::getType)
                    .filter(type -> type != null)
                    .flatMap(type -> Arrays.stream(type.getMethods()))
                    .filter(method -> method.isAnnotationPresent(McpTool.class))
                    .count();
            assertEquals(14, toolCount);
        }
    }
}
