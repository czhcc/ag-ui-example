package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.RunScope;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Stores a surface and its creating run. Replacement must be atomic per surface ID. */
public interface SurfaceRegistry {
    record Entry(RunScope owner, SurfaceSpec spec) {
        public Entry {
            Objects.requireNonNull(owner, "owner must not be null");
            Objects.requireNonNull(spec, "spec must not be null");
        }
    }

    void register(Entry entry);

    Entry replace(String surfaceId, UnaryOperator<Entry> change);
}
