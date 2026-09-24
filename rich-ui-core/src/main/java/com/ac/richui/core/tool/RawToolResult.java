package com.ac.richui.core.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-neutral projection of a native tool result. Structured content is
 * preferred; text is retained for plain tools and JSON fallback parsing.
 */
public record RawToolResult(
        boolean error,
        Map<String, Object> structuredContent,
        String textContent) {

    public RawToolResult {
        structuredContent = structuredContent == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(structuredContent));
        textContent = textContent == null ? "" : textContent;
    }

    public boolean hasStructuredContent() {
        return !structuredContent.isEmpty();
    }

    public boolean hasTextContent() {
        return !textContent.isBlank();
    }
}
