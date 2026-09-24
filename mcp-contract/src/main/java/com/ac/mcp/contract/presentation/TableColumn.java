package com.ac.mcp.contract.presentation;

/**
 * 表格列所对应的业务字段及其显示名称。
 */
public record TableColumn(String field, String label) {

    /**
     * 校验字段名和显示名称均非空。
     */
    public TableColumn {
        field = MappingFields.required(field, "field");
        label = MappingFields.required(label, "label");
    }
}
