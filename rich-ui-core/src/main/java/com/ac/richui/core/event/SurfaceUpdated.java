package com.ac.richui.core.event;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import java.time.Instant;
import java.util.Objects;

/** A validated replacement of an existing surface. */
public record SurfaceUpdated(SurfaceSpec surface, Instant timestamp) implements RichRuntimeEvent {
    public SurfaceUpdated {
        Objects.requireNonNull(surface, "surface must not be null");
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }
    public SurfaceUpdated(SurfaceSpec surface) { this(surface, Instant.now()); }
    @Override public String type() { return "ui.surface.update"; }
}
