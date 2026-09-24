package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TableMapping(List<TableColumn> columns) implements ViewMapping {

    public TableMapping {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }

    public static TableMapping auto() {
        return new TableMapping(List.of());
    }

    /** Interprets legacy map entries as field-to-label pairs. */
    public static TableMapping from(Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return auto();
        }
        return new TableMapping(mapping.entrySet().stream()
                .map(entry -> new TableColumn(entry.getKey(), entry.getValue()))
                .toList());
    }

    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(columns.stream().map(TableColumn::field).toArray(String[]::new));
    }
}
