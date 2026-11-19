package com.ac.mcpapps.core;

/** Public error with a stable code; transport details must never be returned to the View. */
public final class McpAppException extends RuntimeException {
    private final String code;
    public McpAppException(String code) { super(code); this.code = code; }
    public String code() { return code; }
}
