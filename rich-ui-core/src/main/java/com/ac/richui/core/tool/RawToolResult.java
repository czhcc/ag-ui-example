package com.ac.richui.core.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 原生工具结果的公共表示；优先使用结构化内容，文本供普通工具和降级解析使用。
 */
public record RawToolResult(
        boolean error,
        Map<String, Object> structuredContent,
        String textContent) {

    /**
     * 保存结构化内容的不可变副本，并规范化空文本。
     */
    public RawToolResult {
        structuredContent = structuredContent == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(structuredContent));
        textContent = textContent == null ? "" : textContent;
    }

    /**
     * 判断是否存在结构化内容。
     */
    public boolean hasStructuredContent() {
        return !structuredContent.isEmpty();
    }

    /**
     * 判断是否存在非空文本内容。
     */
    public boolean hasTextContent() {
        return !textContent.isBlank();
    }
}
