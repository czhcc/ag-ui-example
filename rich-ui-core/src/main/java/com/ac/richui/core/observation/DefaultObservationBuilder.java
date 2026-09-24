package com.ac.richui.core.observation;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.text.BoundedText;
import com.ac.richui.core.text.SensitiveTextSanitizer;
import com.ac.richui.core.text.TextSanitizer;
import com.ac.richui.core.tool.ToolIdentity;
import java.util.ArrayList;
import java.util.Objects;

/** Builds a bounded observation and never copies full result data. */
public final class DefaultObservationBuilder implements ObservationBuilder {
    private final ObservationPolicy policy;
    private final TextSanitizer sanitizer;

    public DefaultObservationBuilder() {
        this(ObservationPolicy.defaults(), new SensitiveTextSanitizer());
    }

    public DefaultObservationBuilder(ObservationPolicy policy, TextSanitizer sanitizer) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer must not be null");
    }

    @Override
    public AgentObservation build(RunScope scope, ToolIdentity tool,
                                  ResultReference reference, McpResult<?> result) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        Objects.requireNonNull(result, "result must not be null");
        var lines = new ArrayList<String>();
        if (!result.success()) {
            lines.add("工具返回业务失败。");
            if (result.error() != null) {
                lines.add("错误代码: " + safe(result.error().code(), policy.maximumErrorChars()));
                lines.add("错误信息: " + safe(result.error().message(), policy.maximumErrorChars()));
                lines.add("可重试: " + result.error().retryable());
            }
        } else {
            lines.add("工具执行成功。");
            if (result.summary() != null) {
                add(lines, result.summary().description(), policy.maximumHighlightChars());
                if (result.summary().count() != null) {
                    lines.add("结果数量: " + result.summary().count());
                }
                result.summary().highlights().stream()
                        .limit(policy.maximumHighlights())
                        .map(value -> safe(value, policy.maximumHighlightChars()))
                        .filter(value -> !value.isBlank())
                        .forEach(lines::add);
            }
        }
        if (reference != null) {
            lines.add("resultRef: " + reference.value());
        }
        if (result.success() && !result.presentation().views().isEmpty()) {
            lines.add("可用展示视图（必须选择一个）:");
            result.presentation().views().stream().limit(policy.maximumViews()).forEach(view -> lines.add(
                    "viewId=" + safe(view.id(), 120)
                            + ", type=" + safe(view.type(), 40)
                            + ", subType=" + safe(view.subType(), 40)
                            + ", title=" + safe(view.title(), 160)));
            lines.add("必须调用一次 ui_render，使用上述 resultRef 和一个 viewId；不要只返回文字。");
        }
        return new AgentObservation(BoundedText.sanitizeAndLimit(
                String.join("\n", lines), sanitizer, policy.maximumChars()));
    }

    private void add(ArrayList<String> lines, String value, int maximumChars) {
        String safe = safe(value, maximumChars);
        if (!safe.isBlank()) {
            lines.add(safe);
        }
    }

    private String safe(String value, int maximumChars) {
        return BoundedText.sanitizeAndLimit(value, sanitizer, maximumChars);
    }
}
