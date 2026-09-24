package com.ac.richui.core.result;

public final class ResultTooLargeException extends ResultStoreException {
    public ResultTooLargeException() { super("Result exceeds the configured size limit"); }
}
