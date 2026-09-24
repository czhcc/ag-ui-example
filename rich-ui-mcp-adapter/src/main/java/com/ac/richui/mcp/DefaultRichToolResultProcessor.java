package com.ac.richui.mcp;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.observation.AgentObservation;
import com.ac.richui.core.observation.ObservationBuilder;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.result.ResultStore;
import com.ac.richui.core.text.BoundedText;
import com.ac.richui.core.text.SensitiveTextSanitizer;
import com.ac.richui.core.text.TextSanitizer;
import com.ac.richui.core.tool.ProcessedToolResult;
import com.ac.richui.core.tool.RawToolResult;
import com.ac.richui.core.tool.RichToolResultProcessor;
import com.ac.richui.core.tool.ToolIdentity;

import java.util.Objects;

/**
 * 供不同 Runtime 工具包装层共用的 MCP Rich Result 处理流程。
 */
public final class DefaultRichToolResultProcessor implements RichToolResultProcessor {
    private static final int MAXIMUM_PLAIN_OBSERVATION_CHARS = 4_000;
    private static final int MAXIMUM_PUBLIC_SUMMARY_CHARS = 500;

    private final McpResultDecoder decoder;
    private final ResultStore resultStore;
    private final ObservationBuilder observations;
    private final TextSanitizer sanitizer;

    /**
     * 使用默认敏感文本脱敏器创建结果处理器。
     */
    public DefaultRichToolResultProcessor(McpResultDecoder decoder,
                                          ResultStore resultStore,
                                          ObservationBuilder observations) {
        this(decoder, resultStore, observations, new SensitiveTextSanitizer());
    }

    /**
     * 使用指定解码器、存储、观测构建器和脱敏器创建处理器。
     */
    public DefaultRichToolResultProcessor(McpResultDecoder decoder,
                                          ResultStore resultStore,
                                          ObservationBuilder observations,
                                          TextSanitizer sanitizer) {
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
        this.resultStore = Objects.requireNonNull(resultStore, "resultStore must not be null");
        this.observations = Objects.requireNonNull(observations, "observations must not be null");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer must not be null");
    }

    /**
     * 解码工具结果，并为各类结果生成安全观测及必要的存储引用。
     */
    @Override
    public ProcessedToolResult process(RunScope scope, ToolIdentity tool, RawToolResult raw) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        DecodedMcpResult decoded = decoder.decode(raw);
        return switch (decoded.kind()) {
            case MCP_ERROR, INVALID_RICH_RESULT -> plain(
                    ProcessedToolResult.Kind.MCP_ERROR, decoded.text());
            case PLAIN_RESULT -> plain(ProcessedToolResult.Kind.PLAIN_RESULT, decoded.text());
            case BUSINESS_ERROR -> stored(
                    ProcessedToolResult.Kind.BUSINESS_ERROR, scope, tool, decoded.result());
            case RICH_RESULT -> stored(
                    ProcessedToolResult.Kind.RICH_RESULT, scope, tool, decoded.result());
        };
    }

    private ProcessedToolResult plain(ProcessedToolResult.Kind kind, String text) {
        String safe = BoundedText.sanitizeAndLimit(text, sanitizer, MAXIMUM_PLAIN_OBSERVATION_CHARS);
        String summary = BoundedText.sanitizeAndLimit(text, sanitizer, MAXIMUM_PUBLIC_SUMMARY_CHARS);
        return new ProcessedToolResult(kind, new AgentObservation(safe), null, summary);
    }

    private ProcessedToolResult stored(ProcessedToolResult.Kind kind, RunScope scope,
                                       ToolIdentity tool, McpResult<?> result) {
        ResultReference reference = resultStore.save(scope, tool, result);
        AgentObservation observation = observations.build(scope, tool, reference, result);
        String publicSummary = result.success()
                ? result.summary() == null ? "" : result.summary().description()
                : result.error() == null ? "" : result.error().message();
        publicSummary = BoundedText.sanitizeAndLimit(
                publicSummary, sanitizer, MAXIMUM_PUBLIC_SUMMARY_CHARS);
        return new ProcessedToolResult(kind, observation, reference, publicSummary);
    }
}
