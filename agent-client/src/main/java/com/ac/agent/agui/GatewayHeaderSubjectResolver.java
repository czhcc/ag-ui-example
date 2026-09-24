package com.ac.agent.agui;

import com.ac.agui.protocol.AgUiProtocolException;
import com.ac.agui.web.AgUiSubjectResolver;
import com.ac.richui.core.context.AccessSubject;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/** Identity headers must be stripped and injected by the trusted ingress proxy. */
@Component
public final class GatewayHeaderSubjectResolver implements AgUiSubjectResolver {
    @Override
    public AccessSubject resolve(ServerHttpRequest request) {
        String tenantId = header(request, "X-Tenant-Id");
        String userId = header(request, "X-User-Id");
        return AccessSubject.of(tenantId, userId);
    }

    private String header(ServerHttpRequest request, String name) {
        String value = request.getHeaders().getFirst(name);
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new AgUiProtocolException("UNAUTHENTICATED", name + " must be injected by the trusted gateway");
        }
        return value;
    }
}
