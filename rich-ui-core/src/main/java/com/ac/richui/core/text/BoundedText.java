package com.ac.richui.core.text;

import java.util.Objects;

/**
 * 对文本执行脱敏和长度限制的工具类。
 */
public final class BoundedText {
    private BoundedText() {
    }

    /**
     * 先脱敏再截断文本，超出长度时以省略号结尾。
     */
    public static String sanitizeAndLimit(String value, TextSanitizer sanitizer, int maximumChars) {
        Objects.requireNonNull(sanitizer, "sanitizer must not be null");
        if (maximumChars < 1) {
            throw new IllegalArgumentException("maximumChars must be positive");
        }
        String sanitized = sanitizer.sanitize(value);
        if (sanitized.length() <= maximumChars) {
            return sanitized;
        }
        return sanitized.substring(0, Math.max(0, maximumChars - 1)) + "…";
    }
}
