package com.ac.mcp.contract.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证展示建议和表格字段映射的基本行为。 */
class PresentationHintTest {
    @Test void noneHasNoViews() {
        assertTrue(PresentationHint.none().views().isEmpty());
    }

    @Test void tableMappingCarriesColumns() {
        var mapping = new TableMapping(java.util.List.of(
                new TableColumn("city", "城市"),
                new TableColumn("count", "次数")));

        assertEquals(java.util.Set.of("city", "count"), mapping.referencedFields());
    }
}
