package com.ac.richui.core.result;

import java.util.Objects;

/** Opaque identifier for data stored outside model and event payloads. */
public record ResultReference(String value) {

    public ResultReference {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank() || value.length() > 200) {
            throw new IllegalArgumentException("value must contain 1 to 200 characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
