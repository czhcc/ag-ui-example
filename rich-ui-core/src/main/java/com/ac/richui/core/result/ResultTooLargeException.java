package com.ac.richui.core.result;

/**
 * 单条结果超过配置的存储大小上限时抛出的异常。
 */
public final class ResultTooLargeException extends ResultStoreException {
    /**
     * 创建结果过大异常。
     */
    public ResultTooLargeException() {
        super("Result exceeds the configured size limit");
    }
}
