# rich-ui-core

Runtime-neutral contracts and implementations for rich MCP result processing,
bounded observations, presentation validation/mapping, authorization-aware
result storage, and runtime events.

This module may depend only on the JDK and `mcp-contract`. It must not depend on
Spring AI Alibaba, AgentScope, Spring Web/MVC/WebFlux, or a concrete result-store
implementation. Runtime adapters translate their native types at this boundary.

`InMemoryResultStore` is the local-development implementation of the
`ResultStore` SPI. It enforces TTL, global capacity, per-tenant entry/byte quota,
and a maximum result size. Every read and write carries an explicit `RunScope`;
reads also require a server-authenticated `AccessSubject`.

Distributed implementations use Redis or a database and implement
`SharedResultStore`. Quota checks, ownership checks, expiry, and reference
creation must be atomic across replicas.

Native MCP SDK parsing lives in `rich-ui-mcp-adapter`, not in this module.
