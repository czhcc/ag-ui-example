# AG-UI protocol

This module is the runtime- and transport-neutral AG-UI boundary. It uses the
official `com.ag-ui.community:java-core:0.1.1` message, tool, interrupt and event
models. `AgUiRunAgentInput` adds `parentRunId`, which is part of the wire
protocol but is not present on the official Java `RunAgentInput` record in
0.1.1.

Supported output events are `RUN_*`, `TEXT_MESSAGE_*`, `TOOL_CALL_*`,
`STATE_SNAPSHOT`, and `CUSTOM ui.surface.create`. State delta production is not
yet supported because the SAA adapter currently exposes no mutable shared-state
delta. This is advertised by the server capabilities endpoint rather than
silently emulating a delta.

`tools`, `context`, and `forwardedProps` are untrusted client input. Tool
declarations are validated but are never promoted to server-side callbacks;
only the allow-listed `locale`, `timeZone`, and `clientName` forwarded values
may affect presentation. Tenant and user identity always come from the server
authentication bridge.
