# rich-ui-core

`rich-ui-core` 是可移植 Rich UI 架构的公共业务核心，位于 `mcp-contract` 契约层之上。它将工具结果的保存、面向 Agent 的安全观测、视图校验与映射，以及运行事件抽象成与具体 Agent Runtime 和传输方式无关的能力。应用可以在 Spring AI Alibaba 和 AgentScope 两条 Runtime 分支中复用同一套语义。

## 主要能力

| 包 | 职责 |
| --- | --- |
| `context` | `RunScope` 串联租户、用户、线程、运行和工具调用；`AccessSubject` 表示经服务端认证的访问主体。 |
| `tool` | 定义工具身份、原始工具结果、处理后结果和公共处理入口；MCP SDK 的原生结果由适配层转换成这些类型。 |
| `result` | 定义 `ResultStore`、`SharedResultStore`、结果引用、存储限制和授权异常。`InMemoryResultStore` 提供本地实现。 |
| `observation`、`text` | 从 Rich Result 生成有长度上限的 Agent 观测，进行文本截断和敏感信息脱敏，避免把完整业务数据送入模型或事件流。 |
| `presentation` | 按结果数据校验视图字段及类型，将已授权的视图映射为 `SurfaceSpec`，由传输适配层编码和发送。 |
| `event` | 定义与 Runtime 无关的生命周期事件、Surface 创建事件、自定义事件及按运行范围发布事件的接口。 |

Rich UI 主链路为：适配层提供工具结果 → 保存完整结果到 `ResultStore` 并获得 `ResultReference` → 构建有界 `AgentObservation` → 用户选择视图后由 `PresentationService` 校验并生成 `SurfaceSpec` → 协议与 Web 层向前端发送事件。完整业务数据由结果接口按授权读取，而不放入事件载荷。

## 依赖与模块关系

本模块的**唯一工程内直接编译依赖**是 `mcp-contract`，用于引用 `McpResult`、`ViewHint` 和 `SurfaceSpec` 等契约类型。JDK 提供集合、时间、反射及并发等基础能力。`org.junit.jupiter:junit-jupiter` 仅用于测试。本模块没有其他直接 Maven 依赖。

根据[模块间关系描述](../docs/模块间关系描述.md)，`rich-ui-mcp-adapter`、`ag-ui-protocol`、`ag-ui-spring-web`、`runtime-saa-adapter` 和 `runtime-agentscope-v2` 直接依赖本模块；`agent-client`、`agentscope-client` 则在应用组装时同时直接依赖本模块。`mcp-server` 独立提供工具服务，不依赖本模块。前端使用协议及 SDK，不直接依赖 Java 类。

本模块不得依赖 Spring AI Alibaba、AgentScope、Spring Web/MVC/WebFlux 或具体共享存储实现；Maven Enforcer 规则检查这类依赖边界。运行时适配器负责将各自的原生类型转换为公共类型；原生 MCP SDK 解析位于 `rich-ui-mcp-adapter`。

## 结果存储与安全边界

`InMemoryResultStore` 是 `ResultStore` SPI 的本地开发环境实现，强制执行生存时间（TTL）、全局容量限制、租户级条目/字节配额以及单条结果的最大尺寸限制。所有读写操作均需携带明确的 `RunScope`；读取操作还要求提供经服务器认证的 `AccessSubject`。

分布式实现采用 Redis 或数据库，并实现 `SharedResultStore` 接口；配额检查、所有权校验、过期处理及引用创建等操作必须在各副本间保持原子性。

`AccessSubject` 应由受信入口根据认证信息创建，不应从请求正文中的 `forwardedProps` 等字段构造。结果读取还会校验租户、用户、线程和运行标识，确保引用不能跨运行访问。
