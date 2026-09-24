package com.ac.richui.core.tool;

import com.ac.richui.core.context.RunScope;

/**
 * 各 Runtime 适配层共用的 Rich Result 处理入口。
 */
@FunctionalInterface
public interface RichToolResultProcessor {

    /**
     * 在指定运行范围内处理工具返回值并生成安全结果。
     */
    ProcessedToolResult process(RunScope scope, ToolIdentity tool, RawToolResult result);
}
