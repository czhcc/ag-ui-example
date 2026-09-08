package com.ac.mcp.contract.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresentationHintTest {
    @Test void noneHasNoViews() {
        assertTrue(PresentationHint.none().views().isEmpty());
    }
}
