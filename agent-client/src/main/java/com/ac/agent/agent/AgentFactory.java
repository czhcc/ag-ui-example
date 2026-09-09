package com.ac.agent.agent;

import com.ac.agent.mcp.registry.McpServerRegistry;
import com.ac.agent.mcp.registry.McpToolRegistry;
import com.ac.agent.presentation.tool.UiRenderTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AgentFactory {
    private final ChatClient.Builder chatClientBuilder;
    private final McpServerRegistry servers;
    private final McpToolRegistry tools;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final UiRenderTool uiRenderTool;

    public AgentFactory(ChatClient.Builder chatClientBuilder, McpServerRegistry servers,
                        McpToolRegistry tools, MessageChatMemoryAdvisor memoryAdvisor, UiRenderTool uiRenderTool) {
        this.chatClientBuilder = chatClientBuilder;
        this.servers = servers;
        this.tools = tools;
        this.memoryAdvisor = memoryAdvisor;
        this.uiRenderTool = uiRenderTool;
    }

    public ChatClient create() {
        servers.all().values().stream().filter(it -> it.enabled()).forEach(it -> tools.discover(it.code()));
        List<ToolCallback> callbacks = new ArrayList<>(tools.callbacks());
        callbacks.addAll(List.of(MethodToolCallbackProvider.builder().toolObjects(uiRenderTool).build().getToolCallbacks()));
        return chatClientBuilder.clone()
                .defaultSystem(AgentPrompt.SYSTEM)
                .defaultAdvisors(memoryAdvisor)
                .defaultToolCallbacks(callbacks.toArray(ToolCallback[]::new))
                .build();
    }
}
