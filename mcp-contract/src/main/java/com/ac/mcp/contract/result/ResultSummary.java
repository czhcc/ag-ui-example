package com.ac.mcp.contract.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 结果的数量、截断状态、文字说明和重点摘要。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultSummary(Integer count, Integer total, Boolean truncated,
                            String description, List<String> highlights) {
    /**
     * 将空重点列表规范化为空列表，并保存不可变副本。
     */
    public ResultSummary {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
    }
}
