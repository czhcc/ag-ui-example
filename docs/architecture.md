# Architecture decisions

- `mcp-contract` is framework-neutral and contains semantic business/result contracts only.
- `mcp-server` owns query, aggregation, authorization boundaries and MCP exposure.
- `agent-client` owns dynamic MCP connections, agent adaptation, result retention and UI events.
- `structuredContent` is the primary result path; text JSON exists only for interoperability.
- Presentation hints never contain renderer-specific code or configuration.
- Spring AI Alibaba 1.x and Spring AI 2.x are isolated in separate executable modules.
