package com.ac.richui.core.result;

public final class TenantQuotaExceededException extends ResultStoreException {
    public TenantQuotaExceededException() { super("Tenant result quota exceeded"); }
}
