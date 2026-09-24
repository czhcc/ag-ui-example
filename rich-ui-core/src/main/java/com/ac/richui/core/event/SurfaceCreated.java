package com.ac.richui.core.event;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import java.time.Instant;
import java.util.Objects;

public record SurfaceCreated(SurfaceSpec surface, Instant timestamp) implements RichRuntimeEvent {
    public SurfaceCreated {
        Objects.requireNonNull(surface, "surface must not be null");
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public SurfaceCreated(SurfaceSpec surface) {
        this(surface, Instant.now());
    }

    @Override
    public String type() {
        return "ui.surface.create";
    }
}
