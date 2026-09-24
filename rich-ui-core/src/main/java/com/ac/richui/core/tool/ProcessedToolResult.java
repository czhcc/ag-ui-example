package com.ac.richui.core.tool;

import com.ac.richui.core.observation.AgentObservation;
import com.ac.richui.core.result.ResultReference;

import java.util.Objects;

/**
 * 公共处理器返回给 Runtime 适配层的安全工具结果。
 */
public record ProcessedToolResult(
        Kind kind,
        AgentObservation observation,
        ResultReference resultReference,
        String publicSummary) {

    /**
     * 校验结果种类和观测，并要求 Rich Result 带有结果引用。
     */
    public ProcessedToolResult {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(observation, "observation must not be null");
        publicSummary = publicSummary == null ? "" : publicSummary;
        if (kind == Kind.RICH_RESULT && resultReference == null) {
            throw new IllegalArgumentException("RICH_RESULT requires a resultReference");
        }
    }

    /**
     * 区分 MCP 错误、业务错误、Rich Result 和普通结果。
     */
    public enum Kind {
        MCP_ERROR,
        BUSINESS_ERROR,
        RICH_RESULT,
        PLAIN_RESULT
    }
}
