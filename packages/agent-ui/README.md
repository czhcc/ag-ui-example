# @ac/agent-ui

Vue 3 SDK for the official AG-UI request/event protocol and the `ac.rich-ui/1.0` presentation
profile. The package is runtime-neutral and works with both `agent-client` and `agentscope-client`.

## Stream and reduce a run

```ts
import {createAgentUiState, reduceAgentUiEvent, streamAgent} from '@ac/agent-ui'
import {reactive} from 'vue'

const state = reactive(createAgentUiState())
const input = {
  threadId: 'thread-1',
  runId: 'run-1',
  messages: [{id: 'message-1', role: 'user', content: 'Show recent activity'}],
  tools: [], context: [], state: {}, forwardedProps: {}, resume: [],
}

await streamAgent(input, event => reduceAgentUiEvent(state, event, input.runId), undefined, {
  credentials: 'include',
  headers: {'X-Tenant-Id': 'local-tenant', 'X-User-Id': 'local-user'}, // local dev only
})
```

`reduceAgentUiEvent` indexes state by `runId`, `messageId`, `toolCallId`, and `surfaceId`. It
supports Run, Text, Tool, State Snapshot/Delta, Reasoning, HITL interrupts, `subagent.*`, and these
custom events:

- `ui.surface.create`
- `ui.surface.update`
- `ui.surface.remove`

## Configure result access

```ts
import {configure} from '@ac/agent-ui'

configure({
  credentials: 'include',
  resultHeaders: () => ({Authorization: `Bearer ${getAccessToken()}`}),
})
```

Result requests include the owning `threadId` and `runId`. HTTP 403, 404, and 410 are represented
as distinct safe fallback states. Authentication should normally use gateway-injected identity or
same-origin credentials; never ship trusted tenant/user header values in a production browser.

## Profile and renderers

Only structurally valid `ac.rich-ui/1.0` surfaces are rendered. Unknown profiles, unsupported
versions, malformed surfaces, unknown component types, inaccessible results, and expired results
all produce non-executable fallback UI.

```ts
import {registerRenderer} from '@ac/agent-ui'
import HeatmapRenderer from './HeatmapRenderer.vue'

registerRenderer('Heatmap', HeatmapRenderer)
```

Renderers receive only `{component, result}`. Tool-result events retain only `kind`, `summary`, and
`resultRef`; full native Tool Result content is never kept in reducer or execution-log state.
