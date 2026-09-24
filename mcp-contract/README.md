# mcp-contract

`mcp-contract` 是平台的 Rich Result 契约层，定义 MCP 数据工具返回结果和 Rich UI 展示描述的公共格式。它只描述数据结构、版本及基本约束，不承担工具调用、结果存储、Web 传输或界面渲染。不同 Agent Runtime 可以使用同一份契约解释工具结果。

## 提供的内容

- `com.ac.mcp.contract.result`：`McpResult<T>` 统一封装业务数据、摘要、展示建议、元数据及业务错误。当前输出版本为 `1.1`，同时允许读取迁移期的 `1.0` 结果。业务失败使用 `success=false` 和 `error`；MCP 协议或传输错误由 MCP 的 `isError` 表示。
- `com.ac.mcp.contract.presentation`：定义展示模式、图表/表格/时间线/关系图的视图建议与字段映射，以及声明式的 `SurfaceSpec`、`ComponentSpec` 和下钻配置。`SurfaceSpec` 使用 `ac.rich-ui/1.0` Profile，具体事件编码由下游适配层负责。
- `src/main/resources/schema/mcp-result.schema.json`：Rich Result JSON Schema，约束结果结构和版本兼容规则。
- `src/main/resources/schema/ac-rich-ui-profile.schema.json`：AG-UI 自定义 Surface 事件的 Profile Schema，约束创建、更新和移除事件中的扩展字段。
- `src/test/resources/schema`：有效、无效和兼容版本的 JSON 样例；测试按 `manifest.json` 校验样例与 Schema 的一致性。

## 依赖关系

工程内直接依赖：**无**。本模块位于公共依赖链的底层，不依赖 `rich-ui-core`、Agent Runtime、Spring Web 或存储实现。

| 范围 | 依赖 | 用途 |
| --- | --- | --- |
| 编译 | `com.fasterxml.jackson.core:jackson-annotations` | JSON 字段包含规则和多态视图类型声明 |
| 测试 | `org.junit.jupiter:junit-jupiter` | 契约单元测试 |
| 测试 | `com.fasterxml.jackson.core:jackson-databind` | 读取 JSON 测试样例 |
| 测试 | `com.networknt:json-schema-validator` | 校验 Schema 和样例 |

下游关系参见[模块间关系描述](../docs/模块间关系描述.md)：`rich-ui-core` 直接依赖本模块；`agent-client` 和 `agentscope-client` 也直接依赖本模块。`rich-ui-mcp-adapter` 通过 `rich-ui-core` 使用契约，将 MCP SDK 结果解析为公共 Rich Result。独立的 `mcp-server` 当前**没有**对本模块的 Maven 依赖，而是按照这里的 JSON Schema 产生兼容结果。前端通过 AG-UI 与 Rich UI 协议使用这些数据，不直接依赖 Java 类。

## 使用边界

新增字段或调整约束时，应保持 DTO 与对应 JSON Schema 的语义一致，并更新测试样例。契约层不得引入 Spring AI Alibaba、AgentScope、Web 框架或具体存储依赖；模块的 Maven Enforcer 规则会检查这些依赖边界。
