package com.ac.richui.core.result;

import java.util.UUID;

/**
 * 使用 UUID 生成结果引用的默认实现。
 */
public final class UuidResultReferenceGenerator implements ResultReferenceGenerator {
    /**
     * 生成带 result_ 前缀的新结果引用。
     */
    @Override
    public ResultReference next() {
        return new ResultReference("result_" + UUID.randomUUID());
    }
}
