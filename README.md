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
