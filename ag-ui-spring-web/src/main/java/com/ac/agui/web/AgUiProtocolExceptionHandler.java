package com.ac.agui.web;

import com.ac.agui.protocol.AgUiProtocolException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class AgUiProtocolExceptionHandler {
    @ExceptionHandler(AgUiProtocolException.class)
    public ResponseEntity<Map<String, String>> protocol(AgUiProtocolException exception) {
        HttpStatus status = switch (exception.code()) {
            case "UNAUTHENTICATED" -> HttpStatus.UNAUTHORIZED;
            case "RUN_ID_CONFLICT" -> HttpStatus.CONFLICT;
            case "RUN_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "REPLAY_WINDOW_EXPIRED" -> HttpStatus.GONE;
            case "RUN_CAPACITY_EXCEEDED" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Map.of(
                "code", exception.code(), "message", exception.getMessage()));
    }
}
