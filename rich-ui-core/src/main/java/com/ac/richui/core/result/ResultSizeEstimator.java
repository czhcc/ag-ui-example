package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;

/** Estimates retained bytes without coupling the portable core to a JSON implementation. */
@FunctionalInterface
public interface ResultSizeEstimator {
    long estimateBytes(McpResult<?> result, long stopAfterBytes);
}
