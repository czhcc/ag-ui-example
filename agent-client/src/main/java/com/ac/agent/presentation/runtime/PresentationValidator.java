package com.ac.agent.presentation.runtime;

import com.ac.mcp.contract.presentation.ViewHint;
import com.ac.mcp.contract.result.McpResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Map;

@Component
public class PresentationValidator {
    private final ObjectMapper mapper;
    public PresentationValidator(ObjectMapper mapper) { this.mapper = mapper; }
    public void validate(ViewHint view, McpResult<?> result) {
        if (view.mapping().isEmpty()) throw new IllegalArgumentException("View mapping is empty: " + view.id());
        Object sample = result.data();
        if (sample instanceof Collection<?> collection) sample = collection.stream().findFirst().orElse(null);
        if (sample == null) throw new IllegalArgumentException("Cannot render empty data: " + view.id());
        Map<String, Object> fields = mapper.convertValue(sample, new TypeReference<>() { });
        view.mapping().values().forEach(field -> {
            if (!fields.containsKey(field)) throw new IllegalArgumentException("View mapping references missing field: " + field);
        });
    }
}
