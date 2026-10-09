package com.ac.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiExtraBodyTest {
    private final Map<String, Object> defaults = Map.of("chat_template_kwargs", Map.of("enable_thinking", false));

    @Test void modelRequestCarriesThinkingSettingAlongsideToolDefinitions() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var api = api(builder);
        var function = new OpenAiApi.FunctionTool(new OpenAiApi.FunctionTool.Function(
                "Knowledge overview", "kg_knowledge_overview", "{\"type\":\"object\",\"properties\":{}}"));
        var options = OpenAiChatOptions.builder().model("trs-m6").extraBody(defaults)
                .tools(List.of(function)).internalToolExecutionEnabled(false).build();
        var model = OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andExpect(jsonPath("$.chat_template_kwargs.enable_thinking").value(false))
                .andExpect(jsonPath("$.tools[0].function.name").value("kg_knowledge_overview"))
                .andRespond(withSuccess("""
                        {"id":"test","object":"chat.completion","created":1,"model":"trs-m6",
                         "choices":[{"index":0,"message":{"role":"assistant","content":"完成"},"finish_reason":"stop"}]}
                        """, MediaType.APPLICATION_JSON));
        assertEquals("完成", model.call(new Prompt("概览")).getResult().getOutput().getText());
        server.verify();
    }

    @Test void requestOverridesRemainIntactAndOriginalIsNotMutated() throws Exception {
        var api = api(RestClient.builder());
        var original = new ChatCompletionRequest(List.of(), true);
        var restored = api.withDefaultExtraBody(original);
        var json = new ObjectMapper().valueToTree(restored);
        assertFalse(json.path("chat_template_kwargs").path("enable_thinking").asBoolean(true));
        assertTrue(original.extraBody().isEmpty());
        original.extraBody().put("chat_template_kwargs", Map.of("enable_thinking", true));
        assertEquals(Map.of("enable_thinking", true),
                api.withDefaultExtraBody(original).extraBody().get("chat_template_kwargs"));
    }

    private ToolCallMergingOpenAiApi api(RestClient.Builder builder) {
        return new ToolCallMergingOpenAiApi("http://localhost", new NoopApiKey(), new LinkedMultiValueMap<>(),
                "/chat/completions", "/embeddings", builder, WebClient.builder(),
                new DefaultResponseErrorHandler(), defaults);
    }
}
