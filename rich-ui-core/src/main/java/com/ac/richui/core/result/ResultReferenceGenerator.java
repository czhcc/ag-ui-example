package com.ac.richui.core.result;

/**
 * 为存储结果生成不透明引用的接口。
 */
@FunctionalInterface
public interface ResultReferenceGenerator {
    /**
     * 生成下一个结果引用。
     */
    ResultReference next();
}
