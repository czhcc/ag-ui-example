package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.ChartMapping;
import com.ac.mcp.contract.presentation.EntityCardMapping;
import com.ac.mcp.contract.presentation.EvidenceChainMapping;
import com.ac.mcp.contract.presentation.HeatmapMapping;
import com.ac.mcp.contract.presentation.MetricMapping;
import com.ac.mcp.contract.presentation.RelationGraphMapping;
import com.ac.mcp.contract.presentation.RelationshipPathMapping;
import com.ac.mcp.contract.presentation.TableMapping;
import com.ac.mcp.contract.presentation.TimelineMapping;
import com.ac.mcp.contract.presentation.TreeMapping;
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
import java.util.HashSet;
import java.util.Set;

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
        validateOptions(view);
        List<?> rows = rows(result.data());
        if (rows.isEmpty()) {
            throw invalid(view, "cannot render empty data");
        }
        Map<String, ValueKind> observedKinds = new LinkedHashMap<>();
        List<Map<String, ?>> normalized = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, ?> row = row(rows.get(index), view, index);
            normalized.add(row);
            for (String field : view.mapping().referencedFields()) {
                FieldValue resolved = resolve(row, field);
                if (!resolved.present() || resolved.value() == null) {
                    if (optionalValue(view, field, resolved.present())) continue;
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
        validateStructure(view, normalized);
    }

    private void validateOptions(ViewHint view) {
        int maximum;
        String flag;
        switch (view.type()) {
            case "metric" -> { maximum = 12; flag = "showChange"; }
            case "entity_card" -> { maximum = 50; flag = "showSummary"; }
            case "tree" -> { maximum = 500; flag = null; }
            case "heatmap" -> { maximum = 1000; flag = "showLabels"; }
            case "relationship_path" -> { maximum = 30; flag = "showLabels"; }
            case "evidence_chain" -> { maximum = 50; flag = "showSource"; }
            default -> { return; }
        }
        for (var option : view.options().entrySet()) {
            if ("limit".equals(option.getKey())) {
                Object value = option.getValue();
                if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())
                        || n.doubleValue() != n.intValue() || n.intValue() < 1 || n.intValue() > maximum)
                    throw invalid(view, "limit must be an integer from 1 to " + maximum);
            } else if (flag != null && flag.equals(option.getKey())) {
                if (!(option.getValue() instanceof Boolean)) throw invalid(view, flag + " must be boolean");
            } else {
                throw invalid(view, "unsupported option '" + option.getKey() + "'");
            }
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
            case MetricMapping metric -> {
                requireScalar(view, row, metric.label(), index);
                requireNumber(view, row, metric.value(), index);
                if (metric.change() != null && resolve(row, metric.change()).value() != null)
                    requireNumber(view, row, metric.change(), index);
            }
            case EntityCardMapping card -> {
                requireText(view, row, card.id(), index);
                requireText(view, row, card.name(), index);
            }
            case TreeMapping tree -> {
                requireText(view, row, tree.id(), index);
                requireText(view, row, tree.label(), index);
                if (resolve(row, tree.parentId()).value() != null) requireText(view, row, tree.parentId(), index);
                if (tree.hasChildren() != null && resolve(row, tree.hasChildren()).value() != null
                        && !(resolve(row, tree.hasChildren()).value() instanceof Boolean))
                    throw invalid(view, "row " + index + " hasChildren must be boolean");
            }
            case HeatmapMapping heatmap -> {
                requireScalar(view, row, heatmap.x(), index);
                requireScalar(view, row, heatmap.y(), index);
                requireNumber(view, row, heatmap.value(), index);
            }
            case RelationshipPathMapping path -> {
                requireStep(view, row, path.step(), index);
                requireText(view, row, path.source(), index);
                requireText(view, row, path.target(), index);
            }
            case EvidenceChainMapping chain -> {
                requireStep(view, row, chain.step(), index);
                requireText(view, row, chain.id(), index);
                requireText(view, row, chain.title(), index);
                requireText(view, row, chain.source(), index);
            }
        }
    }

    private boolean optionalValue(ViewHint view, String field, boolean present) {
        return switch (view.mapping()) {
            case MetricMapping m -> field.equals(m.unit()) || field.equals(m.change());
            case EntityCardMapping m -> field.equals(m.kind()) || field.equals(m.summary());
            case TreeMapping m -> (present && field.equals(m.parentId())) || field.equals(m.hasChildren());
            case RelationshipPathMapping m -> field.equals(m.label());
            case EvidenceChainMapping m -> field.equals(m.summary()) || field.equals(m.evidenceRef());
            default -> false;
        };
    }

    private void requireText(ViewHint view, Map<String, ?> row, String field, int index) {
        Object value = resolve(row, field).value();
        if (!(value instanceof CharSequence text) || text.toString().isBlank())
            throw invalid(view, "row " + index + " field '" + field + "' must be nonblank text");
    }

    private void requireStep(ViewHint view, Map<String, ?> row, String field, int index) {
        Object value = resolve(row, field).value();
        if (!(value instanceof Number number) || number.intValue() != number.doubleValue() || number.intValue() < 1)
            throw invalid(view, "row " + index + " field '" + field + "' must be a positive integer");
    }

    private void validateStructure(ViewHint view, List<Map<String, ?>> rows) {
        switch (view.mapping()) {
            case EntityCardMapping m -> {
                Set<Object> ids = new HashSet<>();
                for (var row : rows) if (!ids.add(resolve(row, m.id()).value())) throw invalid(view, "duplicate entity id");
            }
            case TreeMapping m -> validateTree(view, rows, m);
            case HeatmapMapping m -> {
                Set<List<Object>> points = new HashSet<>();
                for (var row : rows) if (!points.add(List.of(resolve(row, m.x()).value(), resolve(row, m.y()).value())))
                    throw invalid(view, "duplicate heatmap coordinate");
            }
            case RelationshipPathMapping m -> {
                var ordered = ordered(view, rows, m.step());
                for (int i = 1; i < ordered.size(); i++)
                    if (!Objects.equals(resolve(ordered.get(i - 1), m.target()).value(),
                            resolve(ordered.get(i), m.source()).value())) throw invalid(view, "disconnected path");
            }
            case EvidenceChainMapping m -> {
                ordered(view, rows, m.step());
                Set<Object> ids = new HashSet<>();
                for (var row : rows) if (!ids.add(resolve(row, m.id()).value())) throw invalid(view, "duplicate evidence id");
            }
            default -> { }
        }
    }

    private List<Map<String, ?>> ordered(ViewHint view, List<Map<String, ?>> rows, String stepField) {
        var ordered = new ArrayList<>(rows);
        ordered.sort((a, b) -> Integer.compare(((Number) resolve(a, stepField).value()).intValue(),
                ((Number) resolve(b, stepField).value()).intValue()));
        for (int i = 0; i < ordered.size(); i++)
            if (((Number) resolve(ordered.get(i), stepField).value()).intValue() != i + 1)
                throw invalid(view, "steps must be unique and consecutive from 1");
        return ordered;
    }

    private void validateTree(ViewHint view, List<Map<String, ?>> rows, TreeMapping m) {
        Map<String, String> parents = new LinkedHashMap<>();
        int roots = 0;
        for (var row : rows) {
            String id = String.valueOf(resolve(row, m.id()).value());
            Object parent = resolve(row, m.parentId()).value();
            if (parents.putIfAbsent(id, parent == null ? "" : String.valueOf(parent)) != null)
                throw invalid(view, "duplicate tree id");
            if (parent == null) roots++;
        }
        if (roots == 0) throw invalid(view, "tree requires a root");
        for (String id : parents.keySet()) {
            Set<String> seen = new HashSet<>();
            String cursor = id;
            int depth = 0;
            while (!cursor.isEmpty()) {
                if (!seen.add(cursor)) throw invalid(view, "tree cycle");
                cursor = parents.get(cursor);
                if (cursor == null) throw invalid(view, "orphan tree node");
                if (++depth > 10) throw invalid(view, "tree depth exceeds 10");
            }
        }
    }

    private void requireNumber(ViewHint view, Map<String, ?> row, String field, int index) {
        Object value = resolve(row, field).value();
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
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
