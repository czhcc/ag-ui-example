# ag-ui-protocol

`ag-ui-protocol` 是与 Agent Runtime 和 Web 传输方式无关的 AG-UI 协议层。它解析运行请求，把 `rich-ui-core` 的内部运行事件转换成正式 AG-UI 事件，并编码为协议 JSON。SSE 投递、重放和 HTTP 接口由 `ag-ui-spring-web` 负责。

## 主要职责

| 类型 | 作用 |
| --- | --- |
| `AgUiRunInputDecoder`、`AgUiRunAgentInput` | 严格校验请求中的消息、工具、上下文、恢复数据和长度限制；保留协议字段 `parentRunId`，并可转换为官方 `RunAgentInput`。 |
| `AgUiEventTranslator` | 按运行范围关联消息、工具调用和工具结果，去重 Surface，补齐必要的起始与终态事件，将内部 `RichRuntimeEvent` 映射为官方事件模型。 |
| `AgUiEventEncoder` | 将官方事件编码为字段名与角色值符合 AG-UI 线协议的 JSON。 |
| `AgUiProtocolException` | 表示带错误代码的协议输入错误。 |

当前转换器输出 `RUN_*`、`TEXT_MESSAGE_*`、`TOOL_CALL_*`、`STATE_SNAPSHOT` 和 `CUSTOM ui.surface.create` 等事件。编码器也认识 `STATE_DELTA` 事件类型，但当前转换器不产生状态增量；SAA 适配层尚未提供可变共享状态的增量，服务端能力接口会如实声明这一限制。

本模块使用官方 `com.ag-ui.community:java-core:0.1.1` 的消息、工具、中断及事件模型。该版本的 Java `RunAgentInput` 记录尚不包含线协议中的 `parentRunId`，因此本模块通过 `AgUiRunAgentInput` 保留它。

## 依赖关系

| 范围 | 依赖 | 用途 |
| --- | --- | --- |
| 工程内直接编译依赖 | `rich-ui-core` | `RunScope`、`RichRuntimeEvent` 和 Surface 事件等公共语义 |
| 外部编译依赖 | `com.ag-ui.community:java-core` | 官方 AG-UI 请求、消息、中断和事件类型 |
| 外部编译依赖 | `com.fasterxml.jackson.core:jackson-databind` | 请求解码与事件 JSON 编码 |
| 测试依赖 | `org.junit.jupiter:junit-jupiter` | 输入、事件顺序和协议轨迹测试 |

`mcp-contract` 通过 `rich-ui-core` 间接提供 `SurfaceSpec` 等契约类型。根据[模块间关系描述](../docs/模块间关系描述.md)，`ag-ui-spring-web` 与 `runtime-agentscope-v2` 直接依赖本模块；`agent-client` 和 `agentscope-client` 通过应用组装使用其协议能力。依赖方向保持为协议层 → 公共核心层 → 契约层，本模块不依赖 Spring Web 或具体 Agent Runtime。

## 输入安全边界

`tools`、`context` 和 `forwardedProps` 均是不可信的客户端输入。工具声明会被校验，但不会变成服务端工具回调；只有白名单中的 `locale`、`timeZone` 和 `clientName` 可影响展示。租户和用户身份始终由服务端认证入口建立，不能从请求正文获取。
