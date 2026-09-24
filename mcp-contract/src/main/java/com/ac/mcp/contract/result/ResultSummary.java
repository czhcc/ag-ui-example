package com.ac.mcp.contract.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultSummary(Integer count, Integer total, Boolean truncated,
                            String description, List<String> highlights) {
    public ResultSummary {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
    }
}
