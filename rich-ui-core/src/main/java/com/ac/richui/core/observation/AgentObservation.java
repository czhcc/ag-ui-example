package com.ac.richui.core.observation;

import java.util.Objects;

/**
 * 可返回给 Agent Runtime 的有界、已脱敏文本。
 */
public record AgentObservation(String content) {

    /**
     * 校验观测文本非空引用。
     */
    public AgentObservation {
        Objects.requireNonNull(content, "content must not be null");
    }
}
