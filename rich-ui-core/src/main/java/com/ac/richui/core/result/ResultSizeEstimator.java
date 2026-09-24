package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;

/**
 * 在不依赖特定 JSON 实现的情况下估算结果占用字节数。
 */
@FunctionalInterface
public interface ResultSizeEstimator {
    /**
     * 估算结果大小，允许在超过指定字节阈值后停止。
     */
    long estimateBytes(McpResult<?> result, long stopAfterBytes);
}
