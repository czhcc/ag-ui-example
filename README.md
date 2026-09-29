# AI MCP Platform

以 AG-UI 连接 Vue 前端与 Spring AI Alibaba、AgentScope v2 Java 两套 Runtime。MCP Server 返回结构化业务结果，公共模块保存完整数据、生成有界 Observation，并通过 `ui_render` 产生受控富 UI。

## 模块

| 模块 | 职责 |
|---|---|
| mcp-contract | MCP Result、PresentationHint、ac.rich-ui Schema |
| rich-ui-core / rich-ui-mcp-adapter | 归属授权、结果存储、摘要、MCP 解码和展示映射 |
| ag-ui-protocol / ag-ui-spring-web | 标准请求、事件翻译、SSE、Result API |
| runtime-saa-adapter / agent-client | SAA Runtime 与应用，默认 8080 |
| runtime-agentscope-v2 / agentscope-client | AgentScope Runtime 与应用，默认 8082 |
| mcp-server | 独立 MCP Server，默认 8081，路径 /mcp |
| packages/agent-ui / frontend | Vue SDK、事件 reducer、Renderer 与示例应用 |

## 在新工程中接入对话图表

要复用本项目的完整流程（MCP 取数 → Agent 选择视图 → `ui_render` → AG-UI 事件 → 前端绘图），按所选 Agent 框架引入以下逻辑模块：

| 用途 | Spring AI Alibaba 工程 | AgentScope v2 工程 |
|---|---|---|
| MCP 业务结果、`ViewHint`、`SurfaceSpec` 契约 | `mcp-contract` | `mcp-contract` |
| `ResultStore`、Observation、视图校验与映射 | `rich-ui-core` | `rich-ui-core` |
| MCP `CallToolResult` 解析及富结果处理 | `rich-ui-mcp-adapter` | `rich-ui-mcp-adapter` |
| AG-UI 事件转换 | `ag-ui-protocol` | `ag-ui-protocol` |
| `/api/agent` SSE 和 `/api/results` | `ag-ui-spring-web` | `ag-ui-spring-web` |
| 框架专用 Runtime 适配 | **`runtime-saa-adapter`** | **`runtime-agentscope-v2`** |
| Vue 前端 SDK | `packages/agent-ui` | `packages/agent-ui` |

部分公共模块可通过 Maven 传递依赖获得；新工程显式声明这些模块更容易看清依赖边界。两套框架只选择对应的 Runtime 适配模块，不要把另一套 Runtime 引入依赖树。`ag-ui-spring-web` 使用 Spring WebFlux；若已有自己的 AG-UI Web 入口，需要自行对接事件流和结果读取接口。前端不是 Vue 时，需要自行消费 `CUSTOM ui.surface.create`、`ac.rich-ui` Profile 和结果 API。

**模块依赖不等于完成应用装配。**`agent-client` 与 `agentscope-client` 是可参考的应用，不是通用 Starter：

- Spring AI Alibaba：参考 [AgentFactory](agent-client/src/main/java/com/ac/agent/agent/AgentFactory.java) 注册 MCP `ToolCallback`、`ui_render` 与 `SaaToolEventInterceptor`；参考 [DynamicMcpToolCallback](agent-client/src/main/java/com/ac/agent/mcp/tool/DynamicMcpToolCallback.java) 将 MCP 返回值送入公共结果处理器，并配置 `AgUiRunHandler`、Result API、模型和存储。
- AgentScope v2：参考 [AgentScopeMcpRegistry](agentscope-client/src/main/java/com/ac/agentscopeclient/mcp/AgentScopeMcpRegistry.java) 连接、发现并包装 MCP 工具；参考 [AgentScopeRuntimeConfiguration](agentscope-client/src/main/java/com/ac/agentscopeclient/config/AgentScopeRuntimeConfiguration.java) 将工具与 `AgentScopeUiRenderTool` 注册到 `Toolkit`，并配置 Agent、AG-UI 运行处理器及 Result API。若直接注册框架原生 MCP 工具而不经过富结果包装，就不会得到本项目的 `resultRef` 和 Observation 流程。

`mcp-server` 仅是示例业务工具服务；已有 MCP Server 时无需依赖它。`frontend` 是演示应用；已有 Vue 前端时复用 `packages/agent-ui` 即可。当前 Maven 坐标为 `com.ac:*:1.0.0-SNAPSHOT`，在独立工程中使用前，需要将所需模块加入同一 Maven reactor，或发布到该工程可访问的制品仓库。具体配置和请求示例见[双 Runtime 最小接入](docs/双Runtime最小接入.md)，调用链见[agent-client 调用过程分析](docs/agent-client调用过程分析.md)。

### 前端包与根目录 `package.json`

本仓库的前端由两个包组成：[frontend](frontend/package.json) 是 Vue 演示应用，[packages/agent-ui](packages/agent-ui/package.json) 是可复用的 AG-UI 事件处理与富组件 SDK。`frontend` 通过 `"@ac/agent-ui": "workspace:*"` 引用后者。[pnpm-workspace.yaml](pnpm-workspace.yaml) 声明这两个 workspace 成员，根目录的 `pnpm-lock.yaml` 统一记录它们的依赖和本地链接。

根目录的 [package.json](package.json) 仅声明私有根包的名称和版本，没有前端依赖或构建脚本；它不是 Vue 应用的入口。开发命令定义在 `frontend/package.json`，可在 `frontend` 目录运行 `pnpm dev`，也可从根目录运行 `pnpm --filter ai-mcp-platform-frontend dev`。如果新工程只有一个前端应用、没有独立的本地 SDK 包，则只在前端目录保留 `package.json` 即可；本仓库保留根目录清单是为了明确多包 workspace 的根包身份。

## 接口

`POST /api/agent` 接收标准 `RunAgentInput`，返回 `text/event-stream`。请求必须包含 threadId、runId 和 messages；每个 SSE data 是一个 AG-UI JSON 事件。文本使用 `TEXT_MESSAGE_*`，工具使用 `TOOL_CALL_*`，运行使用 `RUN_*`。

富 UI 使用项目扩展 `CUSTOM ui.surface.create/update/remove`，Profile 为 `ac.rich-ui/1.0`。完整结果通过 `GET /api/results/{resultRef}?threadId=...&runId=...` 读取。旧 `/api/chat/*` 接口已经移除。

`GET /api/agent/capabilities` 声明当前后端能力。SDK 支持的事件范围可能大于后端实际产生的范围，以 capabilities 为准。

## 开发与验收

- [两套 Runtime 的最小接入与请求示例](docs/双Runtime最小接入.md)
- [测试入口、执行条件及当前验收缺口](docs/测试与交付.md)
- [安全、数据分类和存储策略](docs/安全与存储策略.md)
- [可移植方案与实施清单](docs/可移植方案.md)
- [前端 SDK](packages/agent-ui/README.md)

本地内存存储具有 TTL、容量和租户配额。共享存储目前提供 SPI 与启动门禁，尚无内置 Redis 实现；生产多副本部署需完成共享结果、replay 和 Runtime 状态实现及故障恢复验收。可信网关必须校验用户身份并覆盖身份 Header，不能直接信任公网传入的 tenant/user。

本轮未执行构建或测试。新增测试源码、跳过的真实服务用例与通过的测试报告是不同交付状态，详见测试文档。
