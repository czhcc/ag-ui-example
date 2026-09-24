# rich-ui-mcp-adapter

Runtime-neutral MCP SDK adapter. It normalizes `CallToolResult`, gives
`structuredContent` precedence over text JSON fallback, preserves `isError`,
and drives the shared `RichToolResultProcessor` pipeline.

This module must not depend on Spring AI Alibaba, AgentScope, or a web stack.
