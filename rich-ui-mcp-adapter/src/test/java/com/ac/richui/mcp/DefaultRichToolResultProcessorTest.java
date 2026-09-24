package com.ac.richui.mcp;

import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.observation.DefaultObservationBuilder;
import com.ac.richui.core.result.InMemoryResultStore;
import com.ac.richui.core.tool.ProcessedToolResult;
import com.ac.richui.core.tool.RawToolResult;
import com.ac.richui.core.tool.ToolIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 验证处理器保存完整结果，同时仅返回安全观测和结果引用。 */
class DefaultRichToolResultProcessorTest {
    @Test
    void storesFullDataButReturnsOnlySafeObservationAndReference() {
        var store = new InMemoryResultStore(Duration.ofMinutes(5), 10);
        var processor = new DefaultRichToolResultProcessor(
                new DefaultMcpResultDecoder(new ObjectMapper()),
                store,
                new DefaultObservationBuilder());
        var scope = new RunScope("tenant", "user", "thread", "run", "call");
        var tool = new ToolIdentity("crm", "search");
        var raw = new RawToolResult(false, Map.of(
                "specVersion", "1.1",
                "success", true,
                "data", List.of(Map.of("secretRow", "full-data")),
                "summary", Map.of("description", "找到 1 条记录", "highlights", List.of()),
                "presentation", Map.of("mode", "NONE", "views", List.of())), "ignored text");

        ProcessedToolResult processed = processor.process(scope, tool, raw);

        assertEquals(ProcessedToolResult.Kind.RICH_RESULT, processed.kind());
        assertNotNull(processed.resultReference());
        assertFalse(processed.observation().content().contains("full-data"));
        assertEquals(List.of(Map.of("secretRow", "full-data")), store.get(
                processed.resultReference(), scope, AccessSubject.of("tenant", "user")).result().data());
    }
}
