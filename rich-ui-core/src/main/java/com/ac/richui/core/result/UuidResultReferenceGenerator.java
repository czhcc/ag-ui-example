package com.ac.richui.core.result;

import java.util.UUID;

public final class UuidResultReferenceGenerator implements ResultReferenceGenerator {
    @Override
    public ResultReference next() {
        return new ResultReference("result_" + UUID.randomUUID());
    }
}
