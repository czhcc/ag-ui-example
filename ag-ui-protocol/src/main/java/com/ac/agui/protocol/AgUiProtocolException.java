package com.ac.agui.protocol;

/**
 * 携带协议错误代码的 AG-UI 输入异常。
 */
public final class AgUiProtocolException extends RuntimeException {
    private final String code;

    /**
     * 使用错误代码和消息创建协议异常。
     */
    public AgUiProtocolException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 返回便于客户端识别的协议错误代码。
     */
    public String code() {
        return code;
    }
}
