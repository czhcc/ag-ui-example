package com.ac.richui.core.tool;

import com.ac.richui.core.context.RunScope;

/** Shared rich-result pipeline entry point used by every runtime adapter. */
@FunctionalInterface
public interface RichToolResultProcessor {

    ProcessedToolResult process(RunScope scope, ToolIdentity tool, RawToolResult result);
}
