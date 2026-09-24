package com.ac.richui.core.result;

/**
 * 结果存储相关异常的基类。
 */
public class ResultStoreException extends RuntimeException {
    /**
     * 使用指定错误信息创建存储异常。
     */
    public ResultStoreException(String message) {
        super(message);
    }
}
