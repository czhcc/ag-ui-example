package com.ac.richui.core.result;

/**
 * 指定结果引用不存在时抛出的异常。
 */
public final class ResultNotFoundException extends ResultStoreException {
    /**
     * 创建结果不存在异常。
     */
    public ResultNotFoundException() {
        super("Result not found");
    }
}
