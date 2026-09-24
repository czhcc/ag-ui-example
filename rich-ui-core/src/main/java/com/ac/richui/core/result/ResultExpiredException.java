package com.ac.richui.core.result;

public final class ResultExpiredException extends ResultStoreException {
    public ResultExpiredException() {
        super("Result expired");
    }
}
