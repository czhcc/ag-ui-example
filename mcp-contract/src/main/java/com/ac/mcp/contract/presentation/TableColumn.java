package com.ac.mcp.contract.presentation;

public record TableColumn(String field, String label) {

    public TableColumn {
        field = MappingFields.required(field, "field");
        label = MappingFields.required(label, "label");
    }
}
