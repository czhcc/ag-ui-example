package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 描述视图是否允许下钻及其维度、标签和提示词模板。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DrillDown(boolean enabled, String dimension, String label, String promptTemplate) {
    /**
     * 创建禁用下钻的配置。
     */
    public static DrillDown disabled() {
        return new DrillDown(false, null, null, null);
    }
}
