package com.ac.mcp.contract.presentation;

import java.util.List;
import java.util.Objects;

/** Validated presentation contract; transport adapters decide how to encode it. */
public record SurfaceSpec(
        String profile,
        String profileVersion,
        String surfaceId,
        String dataRef,
        List<ComponentSpec> components) {

    public static final String PROFILE = "ac.rich-ui";
    public static final String PROFILE_VERSION = "1.0";

    public SurfaceSpec {
        if (!PROFILE.equals(profile)) {
            throw new IllegalArgumentException("Unsupported profile: " + profile);
        }
        if (!PROFILE_VERSION.equals(profileVersion)) {
            throw new IllegalArgumentException("Unsupported profileVersion: " + profileVersion);
        }
        surfaceId = requireText(surfaceId, "surfaceId");
        dataRef = requireText(dataRef, "dataRef");
        components = components == null ? List.of() : List.copyOf(components);
        if (components.isEmpty()) {
            throw new IllegalArgumentException("components must not be empty");
        }
    }

    public SurfaceSpec(String surfaceId, String dataRef, List<ComponentSpec> components) {
        this(PROFILE, PROFILE_VERSION, surfaceId, dataRef, components);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
