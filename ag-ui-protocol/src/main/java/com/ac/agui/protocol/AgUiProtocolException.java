package com.ac.agui.protocol;

public final class AgUiProtocolException extends RuntimeException {
    private final String code;

    public AgUiProtocolException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
