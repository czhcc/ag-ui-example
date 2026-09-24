# agentscope-client

Independent AgentScope Java v2 validation application. It listens on port `8082` by default and
exposes the same `/api/agent` AG-UI endpoint and `/api/results/{resultRef}` contract as
`agent-client`.

Required request identity headers are `X-Tenant-Id` and `X-User-Id`. Configure model and MCP access
with the `AGENTSCOPE_*` environment variables shown in `application.yml`. The Maven Enforcer rule
rejects any direct or transitive `com.alibaba.cloud.ai` dependency.

The app uses AgentScope `ReActAgent`, Toolkit, MCP client, `RuntimeContext`, Middleware,
`CustomEvent`, and the official `AguiAgentAdapter`. It does not depend on `agent-client`.

Single-process development uses in-memory result, replay, and AgentScope state stores. For a
multi-replica deployment, provide `SharedResultStore`, `SharedAgUiRunStateStore`, and
`SharedAgentStateStore` implementations, then set `ac.deployment.distributed=true`. Startup fails
if any process-local implementation remains active.
