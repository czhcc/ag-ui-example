package com.ac.mcp.contract.result;

public record McpBusinessError(String code, String message, boolean retryable) {
}
