# rich-ui-mcp-adapter

`rich-ui-mcp-adapter` 是 MCP 结果适配层，负责把 MCP SDK 的 `CallToolResult` 转成公共的工具结果模型，并驱动 Rich Result 的解析、保存与安全观测流程。它位于 MCP SDK 和 `rich-ui-core` 之间，不绑定 Spring AI Alibaba、AgentScope 或 Web 传输方式。

## 主要职责

1. `McpCallToolResultAdapter` 将 SDK 的 `CallToolResult` 投影为 `RawToolResult`：保留 `isError`，转换 `structuredContent`，合并文本内容。
2. `DefaultMcpResultDecoder` 按固定优先级解释结果：`isError` 优先；有 `structuredContent` 时优先使用它；否则尝试将文本解析为 JSON；非 JSON 文本保持普通结果。看似 Rich Result 但不满足契约的内容会标记为无效，不当作普通文本继续处理。
3. `DefaultRichToolResultProcessor` 对 Rich Result 和业务失败保存完整结果，生成 `ResultReference` 与有界 `AgentObservation`；对普通结果及 MCP 错误生成脱敏、截断后的观测和公开摘要。完整业务数据由 `ResultStore` 保存，不进入观测文本。

`DecodedMcpResult.Kind` 区分 MCP 错误、无效 Rich Result、业务失败、成功 Rich Result 和普通结果。业务失败属于有效的契约结果，会与成功 Rich Result 一样保存；协议或工具调用错误由 `isError` 表示。

## 依赖关系

| 范围 | 依赖 | 用途 |
| --- | --- | --- |
| 工程内直接编译依赖 | `rich-ui-core` | `RawToolResult`、`ResultStore`、观测、文本脱敏与处理接口 |
| 外部编译依赖 | `io.modelcontextprotocol.sdk:mcp-core` | 接收 MCP SDK 的 `CallToolResult` |
| 外部编译依赖 | `com.fasterxml.jackson.core:jackson-databind` | 转换结构化内容并解析文本 JSON |
| 测试依赖 | `org.junit.jupiter:junit-jupiter` | 解码与处理流程测试 |

`mcp-contract` 是经 `rich-ui-core` 传递的工程内间接依赖，本模块使用其中的 `McpResult`、`PresentationHint` 等契约类型。根据[模块间关系描述](../docs/模块间关系描述.md)，`runtime-agentscope-v2`、`agent-client` 和 `agentscope-client` 直接依赖本模块；其他 Runtime 可以通过各自的工具包装层调用相同的公共处理接口。`mcp-server` 独立产出符合契约的结果，不直接依赖本模块。

## 边界

本模块在入口处把 MCP SDK 返回类型转换为公共类型，避免公共核心直接依赖 SDK。Agent Runtime 的工具注册与调用、AG-UI 事件转换、Web/SSE 传输及结果的具体共享存储实现由相应下游模块负责。本模块的 Maven Enforcer 规则禁止引入 Spring AI Alibaba、AgentScope 和 Spring Web 依赖。
