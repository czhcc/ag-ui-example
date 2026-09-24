package com.ac.agentscopeclient.agui;

import com.ac.agui.protocol.AgUiProtocolException;
import com.ac.agui.web.AgUiSubjectResolver;
import com.ac.richui.core.context.AccessSubject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/** Accepts identity only from headers installed by the trusted ingress. */
@Component
public final class GatewayIdentityResolver implements AgUiSubjectResolver {
    private final String tenantHeader;
    private final String userHeader;

    public GatewayIdentityResolver(
            @Value("${ac.agentscope.security.tenant-header:X-Tenant-Id}") String tenantHeader,
            @Value("${ac.agentscope.security.user-header:X-User-Id}") String userHeader) {
        this.tenantHeader = tenantHeader;
        this.userHeader = userHeader;
    }

    @Override
    public AccessSubject resolve(ServerHttpRequest request) {
        return AccessSubject.of(header(request, tenantHeader), header(request, userHeader));
    }

    private String header(ServerHttpRequest request, String name) {
        String value = request.getHeaders().getFirst(name);
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new AgUiProtocolException("UNAUTHENTICATED", name + " must be injected by the trusted gateway");
        }
        return value;
    }
}
