package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.ChartMapping;
import com.ac.mcp.contract.presentation.RelationGraphMapping;
import com.ac.mcp.contract.presentation.TableMapping;
import com.ac.mcp.contract.presentation.TimelineMapping;
import com.ac.mcp.contract.presentation.ViewHint;
import com.ac.mcp.contract.result.McpResult;

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 校验结果的每行数据、视图字段映射及各视图要求的字段类型。
 */
public final class PresentationValidator {

    /**
     * 验证结果数据是否满足指定视图的渲染要求。
     */
    public void validate(ViewHint view, McpResult<?> result) {
        Objects.requireNonNull(view, "view must not be null");
        Objects.requireNonNull(result, "result must not be null");
        if (!result.success()) {
            throw invalid(view, "cannot render a failed result");
        }
        List<?> rows = rows(result.data());
        if (rows.isEmpty()) {
            throw invalid(view, "cannot render empty data");
        }
        Map<String, ValueKind> observedKinds = new LinkedHashMap<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, ?> row = row(rows.get(index), view, index);
            for (String field : view.mapping().referencedFields()) {
                FieldValue resolved = resolve(row, field);
                if (!resolved.present() || resolved.value() == null) {
                    throw invalid(view, "row " + index + " is missing non-null field '" + field + "'");
                }
                ValueKind kind = ValueKind.of(resolved.value());
                if (kind == ValueKind.STRUCTURED) {
                    throw invalid(view, "field '" + field + "' must be scalar");
                }
                ValueKind previous = observedKinds.putIfAbsent(field, kind);
                if (previous != null && previous != kind
                        && !(previous == ValueKind.NUMBER && kind == ValueKind.NUMBER)) {
                    throw invalid(view, "field '" + field + "' has inconsistent value types");
                }
            }
            validateViewTypes(view, row, index);
        }
    }

    private void validateViewTypes(ViewHint view, Map<String, ?> row, int index) {
        switch (view.mapping()) {
            case ChartMapping chart -> {
                requireScalar(view, row, chart.category(), index);
                requireNumber(view, row, chart.value(), index);
            }
            case TimelineMapping timeline -> {
                requireTime(view, row, timeline.time(), index);
                requireScalar(view, row, timeline.title(), index);
            }
            case RelationGraphMapping graph -> {
                requireScalar(view, row, graph.source(), index);
                requireScalar(view, row, graph.target(), index);
            }
            case TableMapping ignored -> {
            }
        }
    }

    private void requireNumber(ViewHint view, Map<String, ?> row, String field, int index) {
        Object value = resolve(row, field).value();
        if (!(value instanceof Number)) {
            throw invalid(view, "row " + index + " field '" + field + "' must be numeric");
        }
    }

    private void requireTime(ViewHint view, Map<String, ?> row, String field, int index) {
        Object value = resolve(row, field).value();
        if (!(value instanceof CharSequence || value instanceof Number || value instanceof TemporalAccessor)) {
            throw invalid(view, "row " + index + " field '" + field + "' must be a time scalar");
        }
    }

    private void requireScalar(ViewHint view, Map<String, ?> row, String field, int index) {
        Object value = resolve(row, field).value();
        if (ValueKind.of(value) == ValueKind.STRUCTURED) {
            throw invalid(view, "row " + index + " field '" + field + "' must be scalar");
        }
    }

    private List<?> rows(Object data) {
        if (data == null) {
            return List.of();
        }
        if (data instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }
        if (data.getClass().isArray()) {
            int length = Array.getLength(data);
            var rows = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                rows.add(Array.get(data, index));
            }
            return rows;
        }
        return List.of(data);
    }

    private Map<String, ?> row(Object value, ViewHint view, int index) {
        if (value instanceof Map<?, ?> source) {
            var row = new LinkedHashMap<String, Object>();
            source.forEach((key, item) -> row.put(String.valueOf(key), item));
            return row;
        }
        if (value != null && value.getClass().isRecord()) {
            var row = new LinkedHashMap<String, Object>();
            try {
                for (RecordComponent component : value.getClass().getRecordComponents()) {
                    row.put(component.getName(), component.getAccessor().invoke(value));
                }
                return row;
            } catch (ReflectiveOperationException exception) {
                throw invalid(view, "row " + index + " record cannot be inspected");
            }
        }
        throw invalid(view, "row " + index + " must be an object");
    }

    private FieldValue resolve(Map<String, ?> row, String path) {
        if (row.containsKey(path)) {
            return new FieldValue(true, row.get(path));
        }
        Object current = row;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap) || !currentMap.containsKey(part)) {
                return new FieldValue(false, null);
            }
            current = currentMap.get(part);
        }
        return new FieldValue(true, current);
    }

    private IllegalArgumentException invalid(ViewHint view, String detail) {
        return new IllegalArgumentException("Invalid presentation '" + view.id() + "': " + detail);
    }

    private record FieldValue(boolean present, Object value) {
    }

    private enum ValueKind {
        STRING, NUMBER, BOOLEAN, TEMPORAL, OTHER_SCALAR, STRUCTURED;

        private static ValueKind of(Object value) {
            if (value instanceof CharSequence || value instanceof Character || value instanceof Enum<?>) return STRING;
            if (value instanceof Number) return NUMBER;
            if (value instanceof Boolean) return BOOLEAN;
            if (value instanceof TemporalAccessor || value instanceof java.util.Date) return TEMPORAL;
            if (value instanceof Map<?, ?> || value instanceof Collection<?>
                    || (value != null && value.getClass().isArray())) return STRUCTURED;
            return OTHER_SCALAR;
        }
    }
}
