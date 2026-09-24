package com.ac.agui.web;

import com.ac.richui.core.context.AccessSubject;
import org.springframework.http.server.reactive.ServerHttpRequest;

/** Bridges server-authenticated identity into the portable run scope. */
public interface AgUiSubjectResolver {
    AccessSubject resolve(ServerHttpRequest request);
}
