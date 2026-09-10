# AI MCP Platform

面向正式业务演进的 Conversation Driven Analytical UI 基础工程。系统将 Spring AI Agent 与独立的 Spring AI 2 MCP Server 隔离部署，通过动态 MCP Client、结构化结果存储和受控 UI Runtime，把一次对话组织为“查询 → 分析 → 文字 → UI → 继续分析”的连续过程。

## 模块结构

```text
ai-mcp-platform/
├── pom.xml
├── README.md
├── docker-compose.yml
├── docs/
│   └── architecture.md
├── mcp-contract/                 # 纯 Java 协议，不依赖 Spring/MCP
│   └── src/{main,test}/java/com/ac/mcp/contract/
├── mcp-server/                   # Spring AI 2.x 独立应用，端口 8081
│   ├── Dockerfile
│   └── src/{main,test}/...
├── agent-client/                 # Spring AI 1.1 OpenAI 兼容客户端，端口 8080
│   ├── Dockerfile
│   └── src/{main,test}/...
└── frontend/                     # Vue 3 + Vite + TypeScript + Tailwind CSS
    ├── Dockerfile
    └── src/...
```

`mcp-contract` 定义 `McpResult<T>`、摘要、错误、元数据和 renderer-neutral 的 `PresentationHint`。`mcp-server` 负责业务查询、过滤、聚合和 `@McpTool` 暴露。`agent-client` 负责动态连接、工具发现、`ToolCallback` 适配、结果存储、LLM Observation、`ui_render` 和 SSE。

## 调用链

```mermaid
flowchart TD
    U[User] --> A[Spring AI Agent]
    A --> C[DynamicMcpToolCallback]
    C --> M[McpClientManager]
    M --> S[McpSyncClient]
    S --> H[HttpClientStreamableHttpTransport]
    H --> MS[Spring AI 2 MCP Server]
    MS --> T[@McpTool]
    T --> R[McpResult of T]
    R --> SC[structuredContent]
    SC --> C
    C --> RS[ResultStore]
    RS --> O[ObservationBuilder]
    O --> A
    A --> TXT[Text]
    A --> UI[ui_render]
    UI --> PR[PresentationRuntime]
    PR --> EV[UI_CREATE]
    EV --> SSE[SSE]
    SSE --> V[Vue]
```

## 启动方式

准备 Java 21、Maven 3.9+。Agent Client 默认连接 OpenAI 兼容服务
`http://192.168.5.68:6116/v1`，使用模型 `trs-m6`，不发送 API Key。
可通过 `AI_OPENAI_BASE_URL` 和 `AI_OPENAI_MODEL` 环境变量覆盖地址和模型。

先启动 MCP Server：

```powershell
mvn -pl mcp-server -am spring-boot:run
```

默认 Streamable HTTP Endpoint 为 `http://localhost:8081/mcp`。

再启动 Agent Client：

```powershell
mvn -pl agent-client -am spring-boot:run
```

也可在根目录执行 `docker compose up --build`。本工程仅生成了容器配置，初始化阶段未执行镜像构建。

前端本地开发使用 pnpm：

```powershell
cd frontend
pnpm install
pnpm dev
```

页面默认运行在 `http://localhost:5173`，Vite 会把 `/api` 请求代理到 `http://localhost:8080`。通过 Docker Compose 启动时，前端入口为 `http://localhost:3000`。

发起对话：

```http
POST /api/chat/runs
Content-Type: application/json

{"conversationId":"conv-1","message":"统计 person-001 最近一月的活动城市"}
```

前端使用结构化 SSE 对话接口：

```http
POST /api/chat/messages
Content-Type: application/json
Accept: text/event-stream

{"conversationId":"conv-1","message":"继续分析排名第一的城市"}
```

接口依次返回 `delta`、`done` 或 `error` 事件，并使用 `conversationId` 维护最近 20 条消息的会话上下文。

订阅统一 UI/Tool/Text 事件：

```http
GET /api/chat/stream
Accept: text/event-stream
```

## 核心职责

| 类型 | 职责 |
|---|---|
| `McpClientFactory` | 唯一创建 `HttpClientStreamableHttpTransport` 和 `McpSyncClient` 的位置，集中认证 |
| `McpClientManager` | 延迟创建、复用、刷新、移除和关闭多个 MCP Client |
| `McpToolRegistry` | 动态发现工具、应用 prefix、检测名称冲突、维护 callback |
| `DynamicMcpToolCallback` | 调用远程 Tool；分别处理协议错误和业务错误；优先解析 `structuredContent` |
| `ResultStore` | 保存完整结果并生成短期 `resultRef`，默认 Caffeine TTL 45 分钟 |
| `ObservationBuilder` | 只向 LLM 返回摘要、少量 highlights、resultRef 和展示建议 |
| `PresentationRuntime` | 校验 result/view/mapping，构建受控 `SurfaceSpec`，发布 UI Event |
| `ui_render` | Agent Host 的本地 Tool；只接受 `resultRef` 和 `viewId` |
| `AgentEventBus` | 汇聚文本、Tool、UI 和运行状态事件供 SSE 输出 |

## 示例 MCP Tool

`kg_activity_statistics(personId, startTime, endTime)` 在服务端完成聚合，不让 LLM 对大批原始数据做 group/count。Mock 返回：

```json
{
  "specVersion": "1.0",
  "success": true,
  "data": [
    {"city": "北京", "count": 18},
    {"city": "上海", "count": 11},
    {"city": "杭州", "count": 6}
  ],
  "summary": {
    "count": 3,
    "total": 3,
    "truncated": false,
    "description": "按城市统计活动次数",
    "highlights": ["北京18次", "上海11次", "杭州6次"]
  },
  "presentation": {
    "mode": "RECOMMENDED",
    "views": [{
      "id": "city-stat",
      "type": "chart",
      "subType": "bar",
      "title": "各城市活动次数",
      "mapping": {"category": "city", "value": "count"},
      "options": {},
      "priority": 10
    }]
  },
  "resultMeta": null,
  "error": null
}
```

对应的 LLM Observation 不含完整 `data`：

```text
工具 kg_activity_statistics 执行成功。
结果摘要:
按城市统计活动次数
结果数量: 3
北京18次
上海11次
杭州6次
resultRef: result_a12b34c56d78e90f
推荐展示（按需选择，不要为了展示而展示）:
viewId=city-stat, type=chart, subType=bar, title=各城市活动次数
需要展示时调用 ui_render；文字已足够时不要调用。
```

当 LLM 调用 `ui_render(resultRef, "city-stat")`，SSE 会产生类似事件：

```json
{
  "type": "UI_CREATE",
  "surface": {
    "surfaceId": "surface_01",
    "dataRef": "result_a12b34c56d78e90f",
    "components": [{
      "id": "city-stat",
      "type": "Chart",
      "props": {
        "subType": "bar",
        "title": "各城市活动次数",
        "encoding": {"category": "city", "value": "count"}
      }
    }]
  }
}
```

## 协议与安全边界

- `PresentationHint` 只描述 table/chart/relation_graph/timeline 的语义映射，不承载 ECharts、G6、HTML、JavaScript、Vue 或 CSS。
- `CallToolResult.isError` 表示 MCP 协议/执行失败；`McpResult.success=false` 表示业务失败，两者独立处理。
- Tool、SSE 和日志不得输出 Token、连接字符串、密钥、完整敏感数据或内部堆栈。
- 当前 `/mcp` 仅为本地开发放行。部署前必须替换为 JWT/OAuth2，并补 Tool 级授权。

## 当前 TODO

- 根据最终选定的 Spring AI 2 GA/Boot 3 兼容矩阵锁定版本；当前 Server 使用 2.0.0-M5 隔离在独立模块。
- 为 `StoredMcpResult` 注入真实 `conversationId/runId`，并增加按会话授权校验。
- 将事件总线从进程级 multicast 拆为按 run/conversation 隔离的流。
- 添加 MCP Tool enable/disable 持久化配置、配置变更监听和安全的分页发现上限。
- 用 Redis 实现分布式 `ResultStore`，增加容量、审计和脱敏策略。
- 接入真实业务 REST/DB，替换 Mock Service；增加超时、重试、熔断和指标。
- 完成 Vue3 Component Catalog、断线续传、UI_UPDATE 和前端 schema 校验。
- 启用并完善 Client/Server Streamable HTTP 集成测试。

## 根据 mcp-server 的 7 个 MCP 工具（当前为 Mock 数据，实体主要是 `person-001`/`person-002`），可以问这些问题：

### 📊 活动分析（数据最丰富，带图表 ViewHint）

| 问法示例 | 触发工具 | 可视化 |
|---|---|---|
| 统计 person-001 最近一月的活动城市 | `kg_activity_statistics` | 📊 柱状图（北京/上海/杭州） |
| person-001 今年上半年有哪些活动？ | `kg_list_activity` | 🕐 时间线 |
| 按时间线展示 person-001 的活动记录 | `kg_list_activity` | 🕐 时间线 |
| 查一下 person-001 三月份去过哪些地方 | `kg_list_activity` / `kg_activity_statistics` | 文字或图 |

### 🔗 关系网络

| 问法示例 | 触发工具 | 可视化 |
|---|---|---|
| 用关系图展示 person-001 的关系网络 | `kg_expand_relations` | 🕸️ 关系图（G6） |
| person-001 和谁有关系？ | `kg_expand_relations` / `kg_relation_statistics` | 文字/关系图 |
| 统计一下 person-001 的关系分布 | `kg_relation_statistics` | 文字 |

### 🔍 实体查询

| 问法示例 | 触发工具 |
|---|---|
| 帮我搜一下叫“张三”的人 | `kg_search_entity`（关键词任意，Mock 固定返回 person-001） |
| person-001 的详细资料是什么？ | `kg_get_entity` |
| 查一下 person-001 的档案信息 | `kg_get_entity` |

### 📋 证据查询

| 问法示例 | 触发工具 |
|---|---|
| 有哪些证据和 person-001 相关？ | `kg_get_evidence` |
| 查一下 person-001 的证据材料 | `kg_get_evidence` |

### 💡 提问技巧

- **想看图表**：问句里带上“统计/时间线/关系图”等词，或直接说“请给出统计图”——LLM 会调用 `ui_render`
- **实体 ID**：目前 Mock 数据围绕 `person-001`（数据最全），`person-002`/`person-003` 在关系里有出现；问 `person-009` 这类不存在的 ID 会得到业务失败响应
- **组合分析**：可以问综合问题如“分析 person-001 这个人”（LLM 会自主决定调详情+活动+关系多个工具）
- **时间范围**：活动类工具接受 `startTime/endTime` 参数，“最近一月/今年三月/上个月”等自然语言均可

所有回答都是流式文字 + 按需内联图表，数据到 2026 年的 Mock 记录为止。

## 下一阶段建议

先锁定实际仓库与 JDK 上可解析的依赖矩阵并完成编译适配，再实现会话隔离与 Vue3 事件消费。随后接入一个真实只读业务域，打通认证、权限、脱敏、监控和端到端测试，最后再扩展 Redis、多 MCP Server 热更新及更多分析 Skill。
