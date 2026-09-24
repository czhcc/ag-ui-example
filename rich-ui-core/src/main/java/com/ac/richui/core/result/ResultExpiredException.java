package com.ac.richui.core.result;

/**
 * 结果已过期时抛出的异常。
 */
public final class ResultExpiredException extends ResultStoreException {
    /**
     * 创建结果过期异常。
     */
    public ResultExpiredException() {
        super("Result expired");
    }
}
