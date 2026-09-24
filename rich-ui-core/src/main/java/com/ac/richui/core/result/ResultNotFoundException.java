package com.ac.richui.core.result;

public final class ResultNotFoundException extends ResultStoreException {
    public ResultNotFoundException() {
        super("Result not found");
    }
}
