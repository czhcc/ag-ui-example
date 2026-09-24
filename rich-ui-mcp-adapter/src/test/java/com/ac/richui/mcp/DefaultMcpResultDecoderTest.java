package com.ac.richui.mcp;

import com.ac.richui.core.tool.RawToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** 验证 MCP 错误、结构化内容、文本降级及无效契约的解码优先级。 */
class DefaultMcpResultDecoderTest {
    private final DefaultMcpResultDecoder decoder = new DefaultMcpResultDecoder(new ObjectMapper());

    @Test
    void isErrorTakesPrecedenceOverAllPayloads() {
        var decoded = decoder.decode(new RawToolResult(
                true,
                Map.of("success", true, "data", Map.of("ignored", true)),
                "remote failure"));
        assertEquals(DecodedMcpResult.Kind.MCP_ERROR, decoded.kind());
        assertEquals("remote failure", decoded.text());
    }

    @Test
    void structuredContentTakesPrecedenceOverTextFallback() {
        var decoded = decoder.decode(new RawToolResult(
                false,
                Map.of("specVersion", "1.1", "success", true,
                        "data", Map.of("source", "structured"),
                        "presentation", Map.of("mode", "NONE", "views", java.util.List.of())),
                "{\"success\":false}"));
        assertEquals(DecodedMcpResult.Kind.RICH_RESULT, decoded.kind());
        assertEquals(Map.of("source", "structured"), decoded.result().data());
    }

    @Test
    void parsesTextJsonAndPreservesPlainTextFallback() {
        var rich = decoder.decode(new RawToolResult(false, Map.of(),
                "{\"specVersion\":\"1.1\",\"success\":false,"
                        + "\"data\":null,\"presentation\":{\"mode\":\"NONE\",\"views\":[]},"
                        + "\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"missing\",\"retryable\":false}}"));
        assertEquals(DecodedMcpResult.Kind.BUSINESS_ERROR, rich.kind());
        assertFalse(rich.result().success());

        var plain = decoder.decode(new RawToolResult(false, Map.of(), "ordinary result"));
        assertEquals(DecodedMcpResult.Kind.PLAIN_RESULT, plain.kind());
        assertEquals("ordinary result", plain.text());
    }

    @Test
    void rejectsMalformedRichEnvelopeInsteadOfTreatingItAsPlain() {
        var decoded = decoder.decode(new RawToolResult(false,
                Map.of("specVersion", "1.1", "success", false), ""));
        assertEquals(DecodedMcpResult.Kind.INVALID_RICH_RESULT, decoded.kind());
    }
}
