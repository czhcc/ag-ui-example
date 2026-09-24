package com.ac.richui.core.text;

import java.util.List;
import java.util.regex.Pattern;

/** Deterministic baseline redaction for text entering models, events, or logs. */
public final class SensitiveTextSanitizer implements TextSanitizer {
    private static final String REDACTED = "[REDACTED]";
    private static final List<Pattern> SECRET_ASSIGNMENTS = List.of(
            Pattern.compile("(?i)(bearer\\s+)[a-z0-9._~+/-]+=*"),
            Pattern.compile("(?i)((?:api[-_]?key|access[-_]?token|refresh[-_]?token|password|passwd|secret)\\s*[:=]\\s*)[^\\s,;]+"));
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");

    @Override
    public String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        for (Pattern pattern : SECRET_ASSIGNMENTS) {
            sanitized = pattern.matcher(sanitized).replaceAll("$1" + REDACTED);
        }
        sanitized = EMAIL.matcher(sanitized).replaceAll(REDACTED);
        sanitized = MOBILE.matcher(sanitized).replaceAll(REDACTED);
        return ID_CARD.matcher(sanitized).replaceAll(REDACTED);
    }
}
