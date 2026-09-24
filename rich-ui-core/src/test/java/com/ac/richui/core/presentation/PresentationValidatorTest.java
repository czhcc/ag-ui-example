package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.ChartMapping;
import com.ac.mcp.contract.presentation.ChartViewHint;
import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.result.McpResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresentationValidatorTest {
    private final PresentationValidator validator = new PresentationValidator();
    private final ChartViewHint view = new ChartViewHint(
            "city-stat", "chart", "bar", "城市", null,
            new ChartMapping("city", "count"), Map.of(), 0, null);

    @Test
    void validatesEveryRow() {
        var valid = McpResult.success(
                List.of(Map.of("city", "北京", "count", 2), Map.of("city", "上海", "count", 3)),
                null, PresentationHint.recommended(view), null);
        assertDoesNotThrow(() -> validator.validate(view, valid));

        var missingInSecondRow = McpResult.success(
                List.of(Map.of("city", "北京", "count", 2), Map.of("city", "上海")),
                null, PresentationHint.recommended(view), null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(view, missingInSecondRow));
    }

    @Test
    void rejectsWrongViewSpecificType() {
        var invalid = McpResult.success(
                List.of(Map.of("city", "北京", "count", "two")),
                null, PresentationHint.recommended(view), null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(view, invalid));
    }
}
