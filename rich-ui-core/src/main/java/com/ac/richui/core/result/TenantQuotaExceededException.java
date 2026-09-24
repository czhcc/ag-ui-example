package com.ac.richui.core.result;

/**
 * 租户结果条目数或字节配额超限时抛出的异常。
 */
public final class TenantQuotaExceededException extends ResultStoreException {
    /**
     * 创建租户配额超限异常。
     */
    public TenantQuotaExceededException() {
        super("Tenant result quota exceeded");
    }
}
