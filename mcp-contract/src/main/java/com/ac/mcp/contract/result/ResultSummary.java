package com.ac.mcp.contract.result;

import java.util.List;

public record ResultSummary(Integer count, Integer total, Boolean truncated,
                            String description, List<String> highlights) {
    public ResultSummary {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
    }
}
