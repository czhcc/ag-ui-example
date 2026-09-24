# Spring AI Alibaba runtime adapter

This module is the only project module allowed to depend on Spring AI Alibaba.
It translates project-owned `RunScope` and `RuntimeEventSink` contracts to the
SAA Graph runtime without leaking SAA types into `rich-ui-core`.

Version baseline:

- Spring Boot 3.5.8
- Spring AI 1.1.2
- Spring AI Alibaba 1.1.2.2
- MCP Java SDK 0.17.0 (the version used by Spring AI MCP 1.1.2)

Runtime mapping:

| SAA lifecycle | Internal event |
| --- | --- |
| graph subscribed | `run.started` / `run.resumed` |
| streamed message | `text.message.start/content/end` |
| tool interceptor | `tool.call.start/end` |
| rich callback result | `tool.result` |
| HumanInTheLoopHook interruption | `run.interrupted` |
| cancellation signal | `run.cancelled` |
| graph failure/completion | `run.error` / `run.finished` |

`RunnableConfig.threadId` is the conversation checkpoint namespace. The
application `runId` remains immutable metadata for correlation; checkpoint IDs
are generated and managed by the configured SAA saver. `MemorySaver` is the
local implementation and can be replaced by a durable SAA saver without
changing this adapter API.

HITL tools are selected with `ac.agent.hitl.approval-tools` (or
`AI_HITL_APPROVAL_TOOLS`). Cancellation preserves the complete ownership tuple.
AG-UI resume creates a new run in the same tenant/user/thread and identifies the
interrupted run with `parentRunId`; the checkpoint remains thread-scoped.
