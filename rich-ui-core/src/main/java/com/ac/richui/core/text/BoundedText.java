package com.ac.richui.core.text;

import java.util.Objects;

public final class BoundedText {
    private BoundedText() { }

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
