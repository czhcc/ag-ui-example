# runtime-agentscope-v2

AgentScope Java 2.x runtime adapter. It contains no Spring or Spring AI Alibaba dependency.

The call path is:

`RuntimeContext -> AgentScopeRichMiddleware -> AgentTool -> RichToolResultProcessor -> AgentScope CustomEvent -> AguiAgentAdapter`.

`tenantId/userId/threadId/runId/toolCallId` are taken only from the trusted `RunScope` stored as a typed
`RuntimeContext` attribute. `ui_render` emits `ui.surface.create`; the payload remains the shared
`ac.rich-ui` `SurfaceSpec`.
