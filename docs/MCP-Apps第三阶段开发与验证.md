# MCP Apps 第三阶段开发与验证

本实现对应《富可视化流式响应第三阶段实施设计》的通用 Host 和资金流样例。MCP Server 提供固定版本的完整 HTML 资源；Agent 客户端负责身份、工具授权、实例和结果保存；浏览器 Host 负责隔离和桥接。Agent 不生成业务页面。

## 已实现范围

- `mcp-apps-core`：工具 Catalog、资源校验、实例存储 SPI、身份及策略复核、查询幂等与 revision、每用户每分钟 30 次查询、一次性继续分析引用。
- `mcp-apps-spring-web`：实例 API、SDK 初始化扩展能力适配、App 创建事件和 followup 上下文接入。两个模块由 SAA、AgentScope 客户端使用，MCP Server 不依赖它们。
- 两套运行时保留工具 UI 元数据及受控 `ac.uiData`，隐藏 app-only 工具，成功调用入口工具后发布 `mcp.app.create`；App 查询不触发模型。
- `@ac/agent-ui`：文字/App 混排、描述符校验、官方 AppBridge、来源校验、资源摘要校验、关闭与恢复。聊天示例用 sessionStorage 保存有限历史。
- 独立沙箱代理：不同 origin 的外层代理，以及只有 `allow-scripts` 的内层 HTML iframe。首版仅允许自包含资源，拒绝外部网络、子框架和设备权限。
- 资金流样例：账户关系图、展开、金额与时间筛选、分页交易、继续分析。数据为确定性演示数据；金额使用十进制字符串，筛选和版本在业务服务端保存。

工具名沿用项目的下划线风格：`kg_fund_flow_graph`、`kg_fund_flow_expand`、`kg_fund_flow_filter`、`kg_fund_flow_transactions`。入口资源 URI 为 `ui://kg/fund-flow/v1/index.html`。

## 构建

在仓库根目录执行，使用 JDK 21、项目可用的 Maven 和 pnpm：

```powershell
pnpm.cmd install --frozen-lockfile
pnpm.cmd build:apps
mvn -pl agent-client,agentscope-client,mcp-server -am package
```

App 构建产物位于 `mcp-server/target/generated-resources/mcp-apps/fund-flow/v1/index.html`，Maven 将它打入服务端 jar。生成 HTML 不提交到源码。若需要 `mvn clean`，先 clean，再构建 App，最后 package；clean 会删除 App 产物。

依赖固定为 Java 客户端 MCP SDK 0.17 和服务端 SDK 2.0，浏览器 ext-apps SDK 1.1.2。由于旧客户端 SDK 无法表达 UI 扩展能力，`McpAppsInitialization` 仅补写 initialize 请求的 `capabilities.extensions`，并保留 HTTP 认证头。升级 SDK 时应重新验证并移除不再需要的适配。

## 本地运行

分别启动 MCP Server、选择一套 Agent 客户端、沙箱代理和前端：

```powershell
java -jar mcp-server/target/mcp-server-1.0.0-SNAPSHOT.jar
# SAA（默认 8080）或 AgentScope（默认 8082）二选一
java -jar agent-client/target/agent-client-1.0.0-SNAPSHOT.jar
# java -jar agentscope-client/target/agentscope-client-1.0.0-SNAPSHOT.jar
node apps/mcp-app-sandbox/server.mjs
pnpm.cmd --filter ai-mcp-platform-frontend dev
```

默认 MCP Server 为 8081、沙箱为 5174、主站为 5173。AgentScope 作为后端时设置 `AGENT_BACKEND_URL=http://localhost:8082`。模型连接使用各客户端现有配置。向 Agent 请求资金流分析并指定实体及时间范围，使其调用入口工具。

配置项：

| 配置 | 作用 |
|---|---|
| `AC_MCP_APP_ALLOWED_TOOLS` | `serverCode:toolName` 白名单，逗号分隔；空值关闭新 App 和现有交互 |
| `VITE_MCP_APP_SANDBOX_URL` | 浏览器代理 URL，必须与主站不同 origin |
| `MCP_APP_SANDBOX_PORT` | 本地沙箱端口，默认 5174 |
| `MCP_APP_PARENT_ORIGINS` | 沙箱允许嵌入它的主站 origin，逗号分隔 |
| `AC_DISTRIBUTED` | 多副本标记；默认本地实例存储在此模式下拒绝启动 |

## 验证

可复制的聊天测试问句及 App 操作场景见 [实施设计第 16.3 节](./富可视化流式响应第三阶段实施设计.md#163-可直接使用的测试问句)，包含标准入口、时间边界、空结果、筛选分页、继续分析及异常场景。优先用 Q01 验证入口，再在两套运行时分别执行自然语言问句。

```powershell
pnpm.cmd --filter ai-mcp-platform-frontend typecheck
pnpm.cmd --filter @ac/agent-ui test
pnpm.cmd test:mcp-apps-browser
mvn -pl agent-client,agentscope-client,mcp-server -am test
```

浏览器测试使用本机 Edge，启动隔离的 19173/19174 测试服务，加载实际构建的 HTML 和官方 Bridge，验证展开不产生 Agent Run、继续分析新建 Run、伪造窗口消息不调用工具、刷新不重复查询、关闭应用。该测试模拟 Host API 和模型 SSE，不应等同于真实模型端到端验收。

真实 MCP 联调测试可在另外启动的 Server 上运行，不调用模型：

```powershell
java -jar mcp-server/target/mcp-server-1.0.0-SNAPSHOT.jar --server.port=18081
# 另一个终端
$env:MCP_APPS_TEST_URL='http://localhost:18081'
mvn -pl mcp-apps-spring-web -am test
```

该测试覆盖 SDK 0.17/2.0 的工具元数据、资源读取、Host 实例授权、实际展开和继续分析引用。业务测试另验证筛选持久化及跨账户/旧版本游标拒绝。Windows 下运行中的 jar 被锁定，重新 package 前应停止相应测试服务。

## 当前边界与生产接入

2026-10-10 验证记录：后端全模块回归通过；SDK 0.17/2.0 真实 MCP 联调通过；SAA 和 AgentScope 连接真实 MCP Server 的启动检查通过；聊天前端类型检查及生产构建通过；跨 origin 的 Edge 浏览器交互测试通过。现有需要模型服务的前端实时测试默认跳过，未计为已验收。

本次提供可运行的单机通用链路和资金流演示，尚不代表设计文档全部生产验收完成：

- 默认实例、分析会话、限流计数及历史为本地状态，重启无法恢复。共享实例存储需要实现原子 `update`、TTL、跨副本幂等与限流，并与共享 ResultStore、会话历史共同部署；只有声明 `shared()` 不能证明实现正确。
- 白名单策略可替换为 `McpAppPolicy` Bean，接入租户及账户 ACL。演示 MCP 工具接收 Host 的 `ac.subject`；生产 MCP 身份必须来源于已认证的传输身份，不能信任公开端点调用者自行声明的 `_meta`。浏览器 API 继续复用项目现有身份解析器。
- 生产沙箱应部署到独立 HTTPS origin，以反向代理提供服务，隔离主站 Cookie，并配置确切的父 origin。当前代理只绑定本地回环地址。
- 资源版本采用 URI 和 SHA-256 固定；同 URI 内容变化立即拒绝。保留旧版本资源、集中撤销和发布回滚需结合部署系统。
- 当前继续分析传递授权摘要、所选节点和最多 20 笔交易，明确不构成完整合计；更大分析应使用受控结果读取工具。
- 两套运行时已接入公共 Host；真实模型的两套完整端到端验收、服务端会话 busy 协调、集中审计及生产持久化仍需完善。证据/时序 App 为后续扩展。
