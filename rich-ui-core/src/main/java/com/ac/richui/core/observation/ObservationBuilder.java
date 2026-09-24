package com.ac.richui.core.observation;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.tool.ToolIdentity;

/**
 * 构建可供模型使用的观测，不复制完整业务数据。
 */
@FunctionalInterface
public interface ObservationBuilder {

    /**
     * 根据运行范围、工具身份和结果生成安全观测。
     */
    AgentObservation build(
            RunScope scope,
            ToolIdentity tool,
            ResultReference reference,
            McpResult<?> result);
}
