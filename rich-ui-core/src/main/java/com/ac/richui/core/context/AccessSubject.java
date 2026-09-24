package com.ac.richui.core.context;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 经服务端认证的访问主体，不应由 forwardedProps 等不可信请求字段构造。
 */
public record AccessSubject(String tenantId, String userId, Set<String> authorities) {

    /**
     * 校验租户和用户标识，并保存权限集合的不可变副本。
     */
    public AccessSubject {
        tenantId = requireText(tenantId, "tenantId");
        userId = requireText(userId, "userId");
        authorities = authorities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(authorities));
    }

    /**
     * 创建不附加权限的访问主体。
     */
    public static AccessSubject of(String tenantId, String userId) {
        return new AccessSubject(tenantId, userId, Set.of());
    }

    /**
     * 判断访问主体是否拥有指定运行范围。
     */
    public boolean owns(RunScope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        return tenantId.equals(scope.tenantId()) && userId.equals(scope.userId());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException(name + " must contain 1 to 128 characters");
        }
        return value;
    }
}
