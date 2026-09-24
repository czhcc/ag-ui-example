package com.ac.richui.core.text;

/**
 * 文本脱敏策略接口。
 */
@FunctionalInterface
public interface TextSanitizer {
    /**
     * 返回脱敏后的文本。
     */
    String sanitize(String value);
}
