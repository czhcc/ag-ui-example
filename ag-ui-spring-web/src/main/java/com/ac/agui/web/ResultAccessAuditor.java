package com.ac.agui.web;

@FunctionalInterface
public interface ResultAccessAuditor {
    void record(ResultAccessAuditEvent event);
}
