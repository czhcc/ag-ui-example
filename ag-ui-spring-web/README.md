# AG-UI Spring Web transport

The endpoint is `POST /api/agent` with the standard `RunAgentInput` JSON shape
and `Accept: text/event-stream`. Every SSE frame contains one compact AG-UI JSON
event in `data:` and an `id:` of `<runId>:<sequence>`; it does not use private
`delta/ui/tool/done` SSE event names. Comment frames are sent every 15 seconds.

Retry the same request with the same `threadId`, `runId`, body, and
`Last-Event-ID` to replay retained events without executing the run again. A
different body with the same run identity returns `409 RUN_ID_CONFLICT`.
Completed streams are retained for 30 minutes by default.

`GET /api/agent/capabilities` reports optional feature support. In particular,
state snapshots are supported and state deltas and frontend tool execution are
currently declared unsupported. `DELETE /api/agent/threads/{threadId}/runs/{runId}`
and closing the owning SSE connection propagate cancellation to the runtime.

`X-Tenant-Id` and `X-User-Id` must be stripped from public traffic and injected
by a trusted ingress/authentication layer. Request state and `forwardedProps`
must never be used as identity or authorization data.

`ResultApiService` is shared by both runtime applications. It enforces exact
tenant/user/thread/run ownership, maps denial/not-found/expiry to 403/404/410,
sets `private, no-store`, applies per-subject rate limiting, and emits
metadata-only access audits. The default auditor hashes identifiers and never
logs result data.

For multiple replicas set `ac.deployment.distributed=true` and provide
`SharedResultStore` plus `SharedAgUiRunStateStore` beans. The application fails
fast if process-local storage is still active.
