package com.ac.agui.web.result;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 默认的元数据审计器，写日志前先对各项标识进行散列。 */
public final class LoggingResultAccessAuditor implements ResultAccessAuditor {
    private static final System.Logger LOG = System.getLogger(LoggingResultAccessAuditor.class.getName());

    /** 将结果访问状态和散列后的标识写入应用日志。 */
    @Override
    public void record(ResultAccessAuditEvent event) {
        LOG.log(System.Logger.Level.INFO,
                "result_access outcome={0} status={1} tenant={2} user={3} thread={4} run={5} result={6}",
                event.outcome(), event.httpStatus(), token(event.tenantId()), token(event.userId()),
                token(event.threadId()), token(event.runId()), token(event.resultRef()));
    }

    private String token(String value) {
        if (value == null || value.isBlank()) return "-";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
