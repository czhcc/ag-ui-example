package com.ac.richui.core.context;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Server-authenticated caller identity. Never construct this value from
 * untrusted request fields such as AG-UI forwardedProps.
 */
public record AccessSubject(String tenantId, String userId, Set<String> authorities) {

    public AccessSubject {
        tenantId = requireText(tenantId, "tenantId");
        userId = requireText(userId, "userId");
        authorities = authorities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(authorities));
    }

    public static AccessSubject of(String tenantId, String userId) {
        return new AccessSubject(tenantId, userId, Set.of());
    }

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
