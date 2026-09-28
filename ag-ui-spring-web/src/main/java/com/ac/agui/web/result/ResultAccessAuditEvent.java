package com.ac.agui.web.result;

import java.time.Instant;

/** 仅包含访问元数据的审计事件，不记录完整结果或工具参数。 */
public record ResultAccessAuditEvent(
        Instant timestamp,
        String tenantId,
        String userId,
        String threadId,
        String runId,
        String resultRef,
        Outcome outcome,
        int httpStatus) {

    /** 结果访问的允许、拒绝、失效及错误状态。 */
    public enum Outcome { ALLOWED, DENIED, NOT_FOUND, EXPIRED, RATE_LIMITED, INVALID, ERROR }
}
