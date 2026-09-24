package com.ac.agent.agent;

import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;

public record AgentContext(String tenantId, String userId, String threadId, String runId) {
    public RunScope scope() { return new RunScope(tenantId, userId, threadId, runId, null); }
    public AccessSubject subject() { return AccessSubject.of(tenantId, userId); }
    public String conversationId() { return threadId; }
}
