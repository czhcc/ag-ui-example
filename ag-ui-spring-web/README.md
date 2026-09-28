# ag-ui-spring-web

`ag-ui-spring-web` 是 AG-UI 的 Web/SSE 传输层。它接收运行请求，使用 `ag-ui-protocol` 解析和编码事件，按租户、用户、线程及运行维护事件流，并提供重连、取消、能力声明和安全的结果读取服务。具体 Agent Runtime 由应用组装时注入的 `AgUiRunHandler` 执行，本模块不选择 Spring AI Alibaba 或 AgentScope。

## 主要能力

源码按职责划分为 `api`（HTTP/SSE 入口与协议异常处理）、`stream`（运行事件存储与重放）、`result`（结果读取、限流与审计）、`spi`（应用注入的运行处理和身份解析接口）及 `config`（组件装配与多副本存储检查）。测试类放在对应包下。

| 组件 | 职责 |
| --- | --- |
| `AgUiController` | 提供 `POST /api/agent`、能力查询和运行取消接口；将已认证主体转换为 `RunScope`，驱动执行和 SSE 响应。 |
| `AgUiRunStateStore`、`AgUiEventStream` | 定义事件存储与实时投递接口及进程内实现；负责运行幂等、事件序号、保留窗口和重放。 |
| `AgUiRunHandler`、`AgUiSubjectResolver` | 由应用提供的运行执行/取消和受信身份解析接口。 |
| `ResultApiService` | 供两套 Runtime 应用的结果查询控制器复用；校验访问主体和运行归属，执行限流、审计及错误映射。 |
| `AgUiWebConfiguration`、`DistributedStorageGuard` | 装配默认 Web 组件，并在多副本模式下检查共享存储实现。 |

`POST /api/agent` 接收标准 `RunAgentInput` JSON，返回 `text/event-stream`。每个 SSE 帧的 `data:` 是一个 AG-UI 事件 JSON，`id:` 为 `<runId>:<sequence>`；每 15 秒发送注释心跳。携带相同 `threadId`、`runId`、请求正文和 `Last-Event-ID` 重试时，可重放保留的事件而不重复执行；同一运行标识对应不同请求正文时返回 `409 RUN_ID_CONFLICT`。完成的事件流默认保留 30 分钟。

`GET /api/agent/capabilities` 声明可选能力：当前支持状态快照，不支持状态增量和前端工具执行。`DELETE /api/agent/threads/{threadId}/runs/{runId}` 或拥有该运行的 SSE 连接关闭时，会向 Runtime 传播取消请求。

## 依赖关系

| 范围 | 依赖 | 用途 |
| --- | --- | --- |
| 工程内直接编译依赖 | `ag-ui-protocol` | 请求解码、事件转换与编码、协议异常 |
| 工程内直接编译依赖 | `rich-ui-core` | `RunScope`、访问主体、运行事件及结果存储接口 |
| 外部编译依赖 | `spring-boot-starter-webflux` | 响应式 HTTP 控制器、SSE 和 Reactor 类型 |
| 外部编译依赖 | `spring-boot-starter-validation` | Web 请求校验支持 |
| 测试依赖 | `spring-boot-starter-test`、`reactor-test` | 控制器、事件流和响应式行为测试 |

根据[模块间关系描述](../docs/模块间关系描述.md)，`agent-client` 和 `agentscope-client` 直接依赖本模块，并分别注入自己的 `AgUiRunHandler`。`mcp-contract` 经公共核心间接提供 Rich Result 契约。依赖方向为 Web 传输层 → 协议层及公共核心层 → 契约层；本模块不依赖任何具体 Agent Runtime。

## 安全与多副本部署

`X-Tenant-Id` 和 `X-User-Id` 必须由受信入口剥离外部同名请求头后再注入；请求中的 `state` 和 `forwardedProps` 不可作为身份或授权依据。

`ResultApiService` 按租户、用户、线程及运行精确校验结果所有权，将拒绝、未找到、过期分别映射为 403、404、410；响应设置 `private, no-store`，并按访问主体限流、记录仅含元数据的审计事件。默认审计器会散列标识，不记录完整结果数据。

多副本部署时设置 `ac.deployment.distributed=true`，并提供 `SharedResultStore` 与 `SharedAgUiRunStateStore` Bean。若仍使用进程内结果或事件存储，应用会在启动时拒绝运行。
