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

先启动 MCP Server，确认 `/actuator/health` 返回 `UP` 后再启动 Agent 客户端；客户端在启动时初始化 MCP 连接，提前启动会因连接拒绝退出。前端也可以使用 `pnpm.cmd --filter ai-mcp-platform-frontend dev:agentscope`，该模式读取已有的 AgentScope 后端配置。

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

### 真实模型浏览器验收

四个服务启动后，显式开启真实模型测试：

```powershell
$env:MCP_APPS_LIVE='1'
pnpm.cmd test:mcp-apps-live
```

该脚本使用真实 Edge、聊天前端、Agent 后端、MCP Server、沙箱和模型，未拦截或模拟 API。默认入口为 `http://127.0.0.1:5173`，可用 `MCP_APPS_LIVE_URL` 覆盖。没有设置 `MCP_APPS_LIVE=1` 时跳过，普通浏览器回归不会调用真实模型。

用例依次执行 Q01、Q07～Q12，包括账户展开、最低金额筛选、分页、切换账户、继续分析以及后续聊天。Q09 在 Q08 后将最低金额恢复为 `0`，以验证账户 A/B 各四笔交易的分页；最低金额为 `3000` 时，整体图保留两笔交易，但账户 A 的明细只有 `12000.00 CNY` 一笔，另一笔 `3800.50 CNY` 属于 B → C。

证据保存在 `test-results/mcp-apps-live/`，包含 `evidence.json` 和 Q01/Q09/Q12 截图。JSON 记录每轮请求、完整 AG-UI 事件、App 查询参数与结果、浏览器脚本错误及会话历史。测试断言不能替代回答内容复核，需检查选中账户、金额、币种、时间和样本范围是否一致。

### 2026-11-19 AgentScope 2.0.5 验收记录

使用 JDK 21、AgentScope Java `2.0.5` 和现有配置中的真实 `trs-m6` 模型执行。业务数据仍为 MCP Server 的确定性演示数据。

| 场景 | 实际结果 |
|---|---|
| Q01 | 实际调用 `kg_fund_flow_graph`，创建唯一资金流 App，文字说明演示数据及交互界面 |
| Q07 | 展开账户 A 后出现账户 C；只增加 MCP 查询，不增加 Agent Run |
| Q08 | 最低金额 `3000` 后图保留 `12000.00 CNY`、`3800.50 CNY`；展开与分页保留筛选 |
| Q09 | 账户 A、B 各分页得到四笔交易；切换账户不携带旧游标 |
| Q10 | 可见用户消息触发同 thread 的新 Run，使用一次性授权引用，回答针对 `account_001` |
| Q11 | 不重新调用工具，能说明账户 A 的四笔交易、时间范围、CNY/USD 和受限样本口径 |
| Q12 | 分币种列举 `12000.00`、`2400.00`、`60.00 CNY` 与 `950.00 USD`，没有跨币种折算 |

最终复验完整流程共四个 Agent Run、十次 App 直接查询，revision 从 2 连续增长至 11，浏览器脚本错误为零。最终 Q10 按 9 月上旬、方向、对手方和分币种样本金额描述，不推断业务模式；Q12 明确样本统计不代表完整账目。后端相关模块编译与回归通过（61 项通过、1 项需独立服务的集成测试跳过）；沙箱浏览器回归覆盖按钮点击和回车筛选。

验收中修复：

- 筛选按钮原先依赖表单提交，被 `allow-scripts` 沙箱阻止。改为按钮/键盘直接调用受控查询，保留沙箱权限。
- AgentScope SDK 将 AG-UI `context` 保存在 RuntimeContext 中，但不会自动交给模型。适配层将经过限长和脱敏的上下文作为用户数据传入，并进入会话记忆。新增实际执行引擎测试，验证上下文送达、下一轮保留及不进入系统指令。
- 提示词原先要求所有富结果调用 `ui_render`，导致模型在 App 已展示时仍声称没有视图。补充 Host 自动展示 App 的规则，并要求区分选中账户与根实体、说明样本计算口径、不仅凭图结构推断资金业务性质。

早期一次 Q10 出现 `INVALID_INPUT_ERROR`；当时适配层丢弃了 SDK 错误详情，无法据此确认根因。后续完整运行未复现，不能将其记为已证明修复。现增加仅服务端 DEBUG 可见的限长、脱敏诊断，浏览器仍只收到错误码；如再次出现，可启用 `--logging.level.com.ac.runtime.agentscope.AgentScopeAgUiEventMapper=DEBUG` 定位。

当前工具环境未提供 IntelliJ 调试器接口；上下文缺失通过真实模型回答及实际 AgentScope 执行引擎的测试模型入参确认，未使用断点证据。

本记录覆盖本次指定的正常链路，不表示 SAA 真实模型验收、全部异常/并发场景或生产部署已完成。

## 当前边界与生产接入

2026-10-10 验证记录：后端全模块回归通过；SDK 0.17/2.0 真实 MCP 联调通过；SAA 和 AgentScope 连接真实 MCP Server 的启动检查通过；聊天前端类型检查及生产构建通过；跨 origin 的 Edge 浏览器交互测试通过。现有需要模型服务的前端实时测试默认跳过，未计为已验收。

本次提供可运行的单机通用链路和资金流演示，尚不代表设计文档全部生产验收完成：

- 默认实例、分析会话、限流计数及历史为本地状态，重启无法恢复。共享实例存储需要实现原子 `update`、TTL、跨副本幂等与限流，并与共享 ResultStore、会话历史共同部署；只有声明 `shared()` 不能证明实现正确。
- 白名单策略可替换为 `McpAppPolicy` Bean，接入租户及账户 ACL。演示 MCP 工具接收 Host 的 `ac.subject`；生产 MCP 身份必须来源于已认证的传输身份，不能信任公开端点调用者自行声明的 `_meta`。浏览器 API 继续复用项目现有身份解析器。
- 生产沙箱应部署到独立 HTTPS origin，以反向代理提供服务，隔离主站 Cookie，并配置确切的父 origin。当前代理只绑定本地回环地址。
- 资源版本采用 URI 和 SHA-256 固定；同 URI 内容变化立即拒绝。保留旧版本资源、集中撤销和发布回滚需结合部署系统。
- 当前继续分析传递授权摘要、所选节点和最多 20 笔交易，明确不构成完整合计；更大分析应使用受控结果读取工具。
- 两套运行时已接入公共 Host；AgentScope 的 Q01、Q07～Q12 真实模型验收见上文。SAA 真实模型验收、其余异常/并发场景、服务端会话 busy 协调、集中审计及生产持久化仍需完善。证据/时序 App 为后续扩展。
