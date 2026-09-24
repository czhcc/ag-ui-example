package com.ac.richui.core.text;

@FunctionalInterface
public interface TextSanitizer {
    String sanitize(String value);
}
