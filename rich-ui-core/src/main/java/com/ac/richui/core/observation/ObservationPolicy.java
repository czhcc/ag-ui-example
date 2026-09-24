package com.ac.richui.core.observation;

/**
 * 限制观测总长度、摘要条数、错误长度及展示视图数量。
 */
public record ObservationPolicy(
        int maximumChars,
        int maximumHighlights,
        int maximumHighlightChars,
        int maximumErrorChars,
        int maximumViews) {

    /**
     * 返回平台默认的观测限制。
     */
    public static ObservationPolicy defaults() {
        return new ObservationPolicy(4_000, 5, 240, 500, 3);
    }

    /**
     * 校验各项观测限制的取值范围。
     */
    public ObservationPolicy {
        if (maximumChars < 1 || maximumHighlights < 0 || maximumHighlightChars < 1
                || maximumErrorChars < 1 || maximumViews < 0) {
            throw new IllegalArgumentException("Observation limits are invalid");
        }
    }
}
