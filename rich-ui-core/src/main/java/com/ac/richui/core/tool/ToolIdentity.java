package com.ac.richui.core.tool;

import java.util.Objects;

/** Runtime-neutral identity of the server and tool that produced a result. */
public record ToolIdentity(String serverCode, String toolName) {

    public ToolIdentity {
        serverCode = requireText(serverCode, "serverCode");
        toolName = requireText(toolName, "toolName");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
