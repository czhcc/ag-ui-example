package com.ac.agui.web.spi;

import com.ac.richui.core.context.AccessSubject;
import org.springframework.http.server.reactive.ServerHttpRequest;

/** 从受信 Web 入口解析服务端认证的访问主体。 */
public interface AgUiSubjectResolver {
    /** 根据服务端认证信息解析租户和用户身份。 */
    AccessSubject resolve(ServerHttpRequest request);
}
