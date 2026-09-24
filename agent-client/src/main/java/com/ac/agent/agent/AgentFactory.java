package com.ac.agent.agent;

import com.ac.agent.mcp.registry.McpServerRegistry;
import com.ac.agent.mcp.registry.McpToolRegistry;
import com.ac.agent.presentation.tool.UiRenderTool;
import com.ac.runtime.saa.SaaToolEventInterceptor;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class AgentFactory {
    private final ChatModel chatModel;
    private final McpServerRegistry servers;
    private final McpToolRegistry tools;
    private final UiRenderTool uiRenderTool;
    private final BaseCheckpointSaver saver;
    private final SaaToolEventInterceptor toolEvents;
    private final List<String> approvalTools;
    private volatile ReactAgent agent;

    public AgentFactory(
            ChatModel chatModel,
            McpServerRegistry servers,
            McpToolRegistry tools,
            UiRenderTool uiRenderTool,
            BaseCheckpointSaver saver,
            SaaToolEventInterceptor toolEvents,
            @Value("${ac.agent.hitl.approval-tools:}") String approvalTools) {
        this.chatModel = chatModel;
        this.servers = servers;
        this.tools = tools;
        this.uiRenderTool = uiRenderTool;
        this.saver = saver;
        this.toolEvents = toolEvents;
        this.approvalTools = Arrays.stream(approvalTools.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    public ReactAgent create() {
        ReactAgent current = agent;
        if (current != null) return current;
        synchronized (this) {
            if (agent == null) agent = build();
            return agent;
        }
    }

    private ReactAgent build() {
        servers.all().values().stream().filter(it -> it.enabled())
                .forEach(it -> tools.discover(it.code()));
        List<ToolCallback> callbacks = new ArrayList<>(tools.callbacks());
        callbacks.addAll(List.of(MethodToolCallbackProvider.builder()
                .toolObjects(uiRenderTool).build().getToolCallbacks()));

        var builder = ReactAgent.builder()
                .name("ac-rich-agent")
                .model(chatModel)
                .systemPrompt(AgentPrompt.SYSTEM)
                .tools(callbacks)
                .saver(saver)
                .interceptors(List.of(toolEvents));
        if (!approvalTools.isEmpty()) {
            HumanInTheLoopHook.Builder hitl = HumanInTheLoopHook.builder();
            approvalTools.forEach(name -> hitl.approvalOn(name,
                    ToolConfig.builder().description("This tool call requires approval").build()));
            builder.hooks(List.of(hitl.build()));
        }
        return builder.build();
    }
}
