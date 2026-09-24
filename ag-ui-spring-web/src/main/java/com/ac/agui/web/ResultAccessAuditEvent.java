package com.ac.agui.web;

import java.time.Instant;

/** Metadata-only audit event. Full result data and tool arguments are deliberately excluded. */
public record ResultAccessAuditEvent(
        Instant timestamp,
        String tenantId,
        String userId,
        String threadId,
        String runId,
        String resultRef,
        Outcome outcome,
        int httpStatus) {

    public enum Outcome { ALLOWED, DENIED, NOT_FOUND, EXPIRED, RATE_LIMITED, INVALID, ERROR }
}
