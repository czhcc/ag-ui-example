package com.ac.mcp.server.exception;

import com.ac.mcp.contract.result.McpBusinessError;
import com.ac.mcp.contract.result.McpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class McpExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(McpExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    McpResult<Void> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Rejected MCP request: {}", exception.getMessage());
        return McpResult.failure(new McpBusinessError("INVALID_ARGUMENT", exception.getMessage(), false), null);
    }
}
