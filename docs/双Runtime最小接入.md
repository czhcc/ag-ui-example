# 双 Runtime 最小接入

使用 Java 21、Maven 3.9+、Node 22.13+ 和 pnpm。模型必须支持流式输出和工具调用。构建与启动命令供允许执行构建的环境使用。

## Spring AI Alibaba

根目录执行 `mvn -pl agent-client -am package -DskipTests`，再运行 `java -jar agent-client/target/agent-client-1.0.0-SNAPSHOT.jar`。通过 `AI_OPENAI_BASE_URL`、`AI_OPENAI_MODEL` 设置模型；认证信息由应用配置或环境注入。默认端口 8080，MCP 地址在 `agent-client/src/main/resources/application.yml` 的 `ac.mcp.servers` 中配置。

扩展入口：`SaaAgUiRunHandler`、`SaaAgentRuntime`、`SaaToolEventInterceptor`。Checkpoint 使用 `BaseCheckpointSaver`；分布式模式需要真正共享的实现，当前默认 MemorySaver 仅供单进程使用。

## AgentScope v2 Java

根目录执行 `mvn -pl agentscope-client -am package -DskipTests`，再运行 `java -jar agentscope-client/target/agentscope-client-1.0.0-SNAPSHOT.jar`。设置 `AGENTSCOPE_OPENAI_BASE_URL`、`AGENTSCOPE_OPENAI_MODEL`、`AGENTSCOPE_OPENAI_API_KEY` 和 `AGENTSCOPE_MCP_BASE_URL`。默认端口 8082。

扩展入口：`AgentScopeAgUiRuntime`、`AgentScopeMcpTool`、`AgentScopeRichMiddleware`。`AgentStateStore` 按 Bean 替换。该应用禁止传递引入 Spring AI Alibaba。

## 公共 MCP Server 和前端

MCP Server 默认监听 8081，Streamable HTTP 路径 `/mcp`；按其独立 `mcp-server/pom.xml` 构建并启动，生产环境需配置认证和工具权限。前端在 `frontend` 目录运行 `pnpm dev` 连接 SAA，运行 `pnpm dev:agentscope` 连接 AgentScope；代理目标由 `AGENT_BACKEND_URL` 配置。

两个应用使用同一个请求契约：

```http
POST /api/agent
Content-Type: application/json
Accept: text/event-stream
X-Tenant-Id: local-tenant
X-User-Id: local-user

{"threadId":"thread-1","runId":"run-1","messages":[{"id":"message-1","role":"user","content":"统计 person-001 的活动城市并展示图表"}],"tools":[],"context":[],"state":{},"forwardedProps":{},"resume":[]}
```

身份 Header 仅用于本地开发或由可信网关注入，生产入口必须删除外部同名 Header。每个新执行使用新 runId；重试使用相同 body 与 runId，通过 Last-Event-ID 请求有限窗口内的重放。

标准事件为 RUN、TEXT_MESSAGE、TOOL_CALL 等；富 UI 通过 `CUSTOM` 的 `name=ui.surface.create` 与 `value.profile=ac.rich-ui`、`profileVersion=1.0` 扩展。该 Profile 是项目协议，不是标准 AG-UI 内置组件。完整结果通过 `/api/results/{resultRef}?threadId=...&runId=...` 按归属鉴权读取。

示例与测试执行方式参见 [测试与交付](测试与交付.md)。
