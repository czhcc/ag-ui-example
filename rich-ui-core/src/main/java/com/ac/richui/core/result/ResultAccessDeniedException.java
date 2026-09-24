package com.ac.richui.core.result;

/**
 * 访问主体或运行范围无权读取结果时抛出的异常。
 */
public final class ResultAccessDeniedException extends ResultStoreException {
    /**
     * 创建结果访问被拒绝异常。
     */
    public ResultAccessDeniedException() {
        super("Result access denied");
    }
}
