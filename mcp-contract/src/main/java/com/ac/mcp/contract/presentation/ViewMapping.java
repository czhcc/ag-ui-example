package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

/** Strongly typed field mapping shared by contract validation and renderers. */
public sealed interface ViewMapping
        permits ChartMapping, TableMapping, TimelineMapping, RelationGraphMapping {

    @JsonIgnore
    Set<String> referencedFields();
}
