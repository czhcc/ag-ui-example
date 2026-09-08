package com.ac.agent.agent;

import com.ac.agent.mcp.registry.McpServerRegistry;
import com.ac.agent.mcp.registry.McpToolRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class AgentFactory {
    private final ChatClient.Builder chatClientBuilder;
    private final McpServerRegistry servers;
    private final McpToolRegistry tools;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public AgentFactory(ChatClient.Builder chatClientBuilder, McpServerRegistry servers,
                        McpToolRegistry tools, MessageChatMemoryAdvisor memoryAdvisor) {
        this.chatClientBuilder = chatClientBuilder;
        this.servers = servers;
        this.tools = tools;
        this.memoryAdvisor = memoryAdvisor;
    }

    public ChatClient create() {
        servers.all().values().stream().filter(it -> it.enabled()).forEach(it -> tools.discover(it.code()));
        ToolCallback[] callbacks = tools.callbacks().toArray(ToolCallback[]::new);
        return chatClientBuilder.clone()
                .defaultSystem(AgentPrompt.SYSTEM)
                .defaultAdvisors(memoryAdvisor)
                .defaultToolCallbacks(callbacks)
                .build();
    }
}
