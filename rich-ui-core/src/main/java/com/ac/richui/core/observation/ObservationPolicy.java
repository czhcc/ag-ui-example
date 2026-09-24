package com.ac.richui.core.observation;

public record ObservationPolicy(
        int maximumChars,
        int maximumHighlights,
        int maximumHighlightChars,
        int maximumErrorChars,
        int maximumViews) {

    public static ObservationPolicy defaults() {
        return new ObservationPolicy(4_000, 5, 240, 500, 3);
    }

    public ObservationPolicy {
        if (maximumChars < 1 || maximumHighlights < 0 || maximumHighlightChars < 1
                || maximumErrorChars < 1 || maximumViews < 0) {
            throw new IllegalArgumentException("Observation limits are invalid");
        }
    }
}
