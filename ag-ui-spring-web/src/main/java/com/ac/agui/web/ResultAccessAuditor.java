package com.ac.agui.web;

/** 记录结果读取元数据的审计接口。 */
@FunctionalInterface
public interface ResultAccessAuditor {
    /** 记录一次结果访问的审计事件。 */
    void record(ResultAccessAuditEvent event);
}
