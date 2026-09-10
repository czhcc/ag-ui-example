package com.ac.mcp.server.exception;

import com.ac.mcp.server.tool.McpResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class McpExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(McpExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    Map<String, Object> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Rejected MCP request: {}", exception.getMessage());
        return McpResults.failure("INVALID_ARGUMENT", exception.getMessage(), false);
    }
}
