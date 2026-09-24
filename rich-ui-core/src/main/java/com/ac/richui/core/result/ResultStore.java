package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.tool.ToolIdentity;

/**
 * 完整工具结果的存储接口；共享存储实现须保留归属和访问校验。
 */
public interface ResultStore {

    /**
     * 保存指定运行范围内的工具结果并返回引用。
     */
    ResultReference save(RunScope scope, ToolIdentity tool, McpResult<?> result);

    /**
     * 按引用读取结果，并校验运行范围与访问主体。
     */
    StoredMcpResult get(ResultReference reference, RunScope scope, AccessSubject subject);

    /**
     * 在授权校验通过后删除结果。
     */
    void remove(ResultReference reference, RunScope scope, AccessSubject subject);
}
