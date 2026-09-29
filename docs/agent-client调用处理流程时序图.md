我会把一次 `/api/agent` 请求拆成两条相连的时序：MCP 工具取数与结果处理，以及 `ui_render` 生成前端事件。先核对 Web 入口和事件流的实际调用关系。

可以。下面这张图按**一次普通对话中，Agent 调用 MCP 工具并生成图表**的顺序画。横向是类及所属模块；`EventStream` 是旁路事件通道，Agent 的工具返回值仍沿原调用栈返回。

```mermaid
sequenceDiagram
    autonumber
    actor FE as 前端
    participant Web as AgUiController<br/>ag-ui-spring-web
    participant Handler as SaaAgUiRunHandler<br/>agent-client
    participant Service as AgentService<br/>agent-client
    participant Runtime as SaaAgentRuntime<br/>runtime-saa-adapter
    participant Agent as ReactAgent / LLM<br/>SAA
    participant Interceptor as SaaToolEventInterceptor<br/>runtime-saa-adapter
    participant Callback as DynamicMcpToolCallback<br/>agent-client
    participant MCP as MCP Server
    participant Pipeline as Adapter + Processor<br/>rich-ui-mcp-adapter
    participant Store as ResultStore<br/>rich-ui-core
    participant Render as UiRenderTool + PresentationService<br/>agent-client / rich-ui-core
    participant Events as AgUiEventStream<br/>ag-ui-spring-web

    FE->>Web: POST /api/agent
    Web->>Events: register(scope)，open(scope)
    Web->>Handler: execute(input, scope)
    Handler->>Service: stream(用户消息, context)
    Service->>Runtime: stream(message, scope)
    Runtime->>Events: run.started / text.message.start
    Runtime->>Agent: agent.stream(message, config)

    Note over Service,Agent: 首次运行时 AgentFactory 构建 ReactAgent，<br/>注册 MCP ToolCallback、ui_render 和 Interceptor

    Agent->>Interceptor: 调用某个 MCP 工具
    Interceptor->>Events: tool.call.start / tool.call.args
    Interceptor->>Callback: handler.call(enriched request)
    Callback->>MCP: callTool(工具名, 参数)
    MCP-->>Callback: CallToolResult

    Callback->>Pipeline: adapt() → process()
    Note over Pipeline,Store: 解析 isError / structuredContent；<br/>识别 Rich Result；保存完整数据；<br/>生成摘要、resultRef 和 AgentObservation
    Pipeline->>Store: save(McpResult)
    Store-->>Pipeline: resultRef
    Pipeline-->>Callback: ProcessedToolResult

    Callback->>Events: tool.result(kind, summary, resultRef)
    Callback-->>Interceptor: AgentObservation.content
    Interceptor->>Events: tool.call.end / tool.result.fallback
    Interceptor-->>Agent: 工具返回文本
    Note over Agent,Events: EventStream 将内部事件转为 AG-UI 事件；<br/>同一工具的 tool.result 与 fallback 去重

    Agent->>Runtime: 流式文字
    Runtime->>Events: text.message.content(delta)

    opt 模型决定展示推荐视图
        Agent->>Interceptor: 调用 ui_render(resultRef, viewId)
        Interceptor->>Events: tool.call.start / tool.call.args
        Interceptor->>Render: handler.call()
        Render->>Store: 按运行和用户身份读取结果
        Store-->>Render: McpResult
        Note over Render: 校验 viewId、字段映射和数据类型，<br/>生成引用数据的 SurfaceSpec
        Render->>Events: SurfaceCreated
        Render-->>Interceptor: UI 已添加
        Interceptor->>Events: tool.call.end / tool.result.fallback
        Interceptor-->>Agent: 工具返回文本
        Agent->>Runtime: 后续解读文字
        Runtime->>Events: text.message.content(delta)
    end

    Runtime->>Events: text.message.end / run.finished
    Events-->>Web: AG-UI 事件，经 SSE 编码
    Web-->>FE: TEXT_MESSAGE_* / TOOL_CALL_* / CUSTOM(ui.surface.create)

    opt 前端收到 ui.surface.create
        FE->>Web: GET /api/results/{resultRef}
        Web->>Store: 授权后读取完整结果
        Store-->>FE: data、summary、resultMeta
        Note over FE: 根据 Surface 的字段映射绘制图表
    end
```

读图时抓住三个返回方向：

- **给 Agent 的返回值**：`DynamicMcpToolCallback → Interceptor → ReactAgent`，内容是 `AgentObservation.content`，供模型继续推理。[DynamicMcpToolCallback.java](/D:/myprog/idea_21/jclaw/ag_ui/ai-mcp-platform/agent-client/src/main/java/com/ac/agent/mcp/tool/DynamicMcpToolCallback.java:69)
- **给前端的实时事件**：各层调用 `RuntimeEventSink.publish`，进入 `AgUiEventStream`，再由 `AgUiController` 作为 SSE 返回。它与工具方法的返回值是两条路径。[AgUiEventStream.java](/D:/myprog/idea_21/jclaw/ag_ui/ai-mcp-platform/ag-ui-spring-web/src/main/java/com/ac/agui/web/stream/AgUiEventStream.java:85) · [AgUiController.java](/D:/myprog/idea_21/jclaw/ag_ui/ai-mcp-platform/ag-ui-spring-web/src/main/java/com/ac/agui/web/api/AgUiController.java:56)
- **给图表的完整数据**：留在 `ResultStore`；SSE 的 Surface 只携带 `dataRef`，前端再通过 `/api/results/{resultRef}` 读取。[UiRenderTool.java](/D:/myprog/idea_21/jclaw/ag_ui/ai-mcp-platform/agent-client/src/main/java/com/ac/agent/presentation/tool/UiRenderTool.java:24) · [ResultController.java](/D:/myprog/idea_21/jclaw/ag_ui/ai-mcp-platform/agent-client/src/main/java/com/ac/agent/web/ResultController.java:21)

图中的 `Adapter + Processor` 展开后是 `McpCallToolResultAdapter → DefaultRichToolResultProcessor → DefaultMcpResultDecoder / ResultStore / DefaultObservationBuilder`；`ui_render` 则通过 `DefaultPresentationService → PresentationValidator → PresentationMapper` 生成 Surface。Agent 的工具和拦截器在 [AgentFactory.java](/D:/myprog/idea_21/jclaw/ag_ui/ai-mcp-platform/agent-client/src/main/java/com/ac/agent/agent/AgentFactory.java:58) 中装配。