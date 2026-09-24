package com.ac.mcp.contract.presentation;

import java.util.List;

public record PresentationHint(PresentationMode mode, List<ViewHint> views) {
    public PresentationHint {
        mode = mode == null ? PresentationMode.NONE : mode;
        views = views == null ? List.of() : List.copyOf(views);
        if (mode == PresentationMode.NONE && !views.isEmpty()) {
            throw new IllegalArgumentException("NONE presentation must not contain views");
        }
        if (mode != PresentationMode.NONE && views.isEmpty()) {
            throw new IllegalArgumentException(mode + " presentation requires at least one view");
        }
    }

    public static PresentationHint none() { return new PresentationHint(PresentationMode.NONE, List.of()); }
    public static PresentationHint recommended(ViewHint... views) {
        return new PresentationHint(PresentationMode.RECOMMENDED, List.of(views));
    }
}
