# ADR-0001: Runtime-neutral rich result and AG-UI architecture

- Status: Accepted
- Date: 2026-09-15
- Tasks: ARCH-01 through ARCH-05

## Context

The current `agent-client` combines Spring AI `ChatClient`, MCP result handling,
presentation, a process-wide event bus, and a private SSE protocol. That design
cannot provide strict AG-UI compatibility or prove that rich-result handling is
portable to AgentScope v2.

## Decision

The project adopts these non-negotiable goals:

1. `agent-client` exposes a formally compatible AG-UI request and event stream.
2. Its primary agent runtime is Spring AI Alibaba ReactAgent/Graph Runtime, not a
   manually orchestrated Spring AI `ChatClient` loop.
3. Rich result and presentation capabilities are runtime-neutral and are reused
   by an independent AgentScope v2 Java application.

The target dependency direction is strictly inward:

```text
frontend / packages/agent-ui
            |
agent-client                 agentscope-client
            |                 |
runtime-saa-adapter          runtime-agentscope-v2
            \                 /
             ag-ui-spring-web
                     |
              ag-ui-protocol
                     |
          rich-ui-mcp-adapter
                     |
              rich-ui-core
                     |
              mcp-contract
```

An application may assemble multiple inward modules. A lower layer must never
depend on a layer above it or on a sibling runtime adapter.

| Module | Owns | Must not depend on |
|---|---|---|
| `mcp-contract` | MCP rich-result DTOs and schemas | Agent runtimes, web stacks, concrete stores |
| `rich-ui-core` | Run/access context, processing/observation/presentation/event SPI | SAA, AgentScope, MVC/WebFlux, concrete stores |
| `rich-ui-mcp-adapter` | Native MCP result projection into core input | SAA, AgentScope |
| `ag-ui-protocol` | AG-UI request/events and protocol validation | Agent runtimes and business tools |
| `ag-ui-spring-web` | HTTP/SSE, authentication bridge, cancellation, Result API | Concrete agent implementations |
| `runtime-saa-adapter` | SAA agent assembly hooks and event translation | AgentScope |
| `runtime-agentscope-v2` | AgentScope middleware, tools, event translation | Spring AI Alibaba |
| `agent-client` | SAA application composition | AgentScope |
| `agentscope-client` | AgentScope application composition | SAA and `agent-client` |

`rich-ui-core` establishes the following runtime-neutral boundary:

- `RunScope` carries tenant, user, thread, run, and optional tool-call
  correlation. It is explicit; no global or implicit context is allowed.
- `AccessSubject` represents server-authenticated identity. Client-controlled
  request fields must never be used to manufacture it.
- `ToolIdentity` identifies the MCP server and tool without an SDK type.
- `RawToolResult` contains only the normalized MCP error flag, structured
  content, and text fallback.
- `ProcessedToolResult` classifies protocol error, business error, rich result,
  or plain result and exposes only an agent-safe observation/public summary plus
  an opaque reference when applicable.
- `RichToolResultProcessor`, `ObservationBuilder`, `PresentationService`, and
  `RuntimeEventSink` accept explicit `RunScope` and expose no runtime or web
  framework types.
- Presentation reads additionally require an `AccessSubject`; ownership and
  run correlation are separate concepts.

Full MCP result data stays behind an opaque `ResultReference`. It must not enter
model observations, normal runtime events, AG-UI tool-result events, or ordinary
application logs.

## Enforcement

`mcp-contract` and `rich-ui-core` run Maven Enforcer `bannedDependencies` during
the Maven `validate` phase. The rule examines transitive dependencies and rejects
Spring AI Alibaba, AgentScope, Spring Web, Spring Data, and common concrete cache
or Redis clients. Normal Java compilation additionally prevents source imports
whose artifacts are absent.

Runtime adapter and application-specific guards will be added with those modules.
An adapter may depend on its own runtime and inward contracts only.

## Incremental adoption

The `CORE-*` extraction is implemented by `mcp-contract`, `rich-ui-core`, and
`rich-ui-mcp-adapter`. The common path now owns authorization-aware result
storage, bounded and redacted observations, presentation validation/mapping, and
MCP result decoding without a global event bus or implicit thread-local context.

The existing `agent-client` runtime path now uses the shared processor through
the SAA adapter. Its public stream is implemented by the independent
`ag-ui-protocol` and `ag-ui-spring-web` modules, with per-owner/thread/run event
isolation and no process-wide multicast bus. Runtime-specific details remain
outside the common modules.

## Consequences

- Both runtimes must translate native SDK objects at their adapter boundary.
- IDs and authenticated ownership become explicit parameters, adding some
  ceremony but making concurrent-run isolation and authorization testable.
- Concrete storage, transport, and runtime dependencies cannot leak into the
  reusable modules.
- The public SPI can evolve before either runtime migration is completed, while
  keeping the old application operational during the transition.
