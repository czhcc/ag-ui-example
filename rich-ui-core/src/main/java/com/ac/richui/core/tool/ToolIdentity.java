package com.ac.richui.core.tool;

import java.util.Objects;

/**
 * 产生结果的 MCP 服务和工具的公共身份。
 */
public record ToolIdentity(String serverCode, String toolName) {

    /**
     * 校验服务代码和工具名称均非空。
     */
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
