package com.ac.richui.core.result;

import java.util.Objects;

/**
 * 指向独立存储结果的不透明引用，供模型和事件载荷引用。
 */
public record ResultReference(String value) {

    /**
     * 校验引用值的长度和非空约束。
     */
    public ResultReference {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank() || value.length() > 200) {
            throw new IllegalArgumentException("value must contain 1 to 200 characters");
        }
    }

    /**
     * 返回引用的字符串值。
     */
    @Override
    public String toString() {
        return value;
    }
}
