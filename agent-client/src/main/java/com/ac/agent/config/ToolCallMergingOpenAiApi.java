package com.ac.agent.config;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionChunk;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionChunk.ChunkChoice;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionFinishReason;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.ChatCompletionFunction;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.ToolCall;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI-compatible API client that merges fragmented streaming tool-call chunks.
 *
 * <p>Necessary workaround: vLLM-based providers emit a tool_calls delta in every
 * stream chunk (first chunk carries the function name, later chunks carry
 * {@code name: null} + argument fragments). Spring AI treats each chunk as a complete
 * tool call, causing {@code ToolCall(name=null)} NPE in DefaultToolCallingManager.
 * Chunks are merged at the transport layer so every consumer downstream
 * (including recursive tool-execution rounds inside OpenAiChatModel) sees
 * well-formed tool calls.</p>
 */
public class ToolCallMergingOpenAiApi extends OpenAiApi {

    public ToolCallMergingOpenAiApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers,
            String completionsPath, String embeddingsPath, RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
        super(baseUrl, apiKey, headers, completionsPath, embeddingsPath, restClientBuilder, webClientBuilder,
                responseErrorHandler);
    }

    @Override
    public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest,
            org.springframework.util.MultiValueMap<String, String> additionalHttpHeader) {
        return super.chatCompletionStream(chatRequest, additionalHttpHeader)
                .transformDeferred(ToolCallMergingOpenAiApi::mergeToolCallChunks);
    }

    private static Flux<ChatCompletionChunk> mergeToolCallChunks(Flux<ChatCompletionChunk> source) {
        State state = new State();
        return source.handle((chunk, sink) -> {
            if (CollectionUtils.isEmpty(chunk.choices())) {
                sink.next(chunk);
                return;
            }
            ChunkChoice choice = chunk.choices().get(0);
            List<ToolCall> toolCalls = choice.delta() != null ? choice.delta().toolCalls() : null;
            boolean finishing = choice.finishReason() != null;

            if (!CollectionUtils.isEmpty(toolCalls)) {
                state.accumulate(toolCalls);
                if (finishing) {
                    sink.next(state.flushInto(chunk));
                }
                return;
            }
            if (finishing && !state.isEmpty()) {
                sink.next(state.flushInto(chunk));
                return;
            }
            sink.next(chunk);
        });
    }

    private static final class State {

        private final List<Merged> merged = new ArrayList<>();

        void accumulate(List<ToolCall> deltas) {
            for (ToolCall delta : deltas) {
                int index = delta.index() != null ? delta.index() : merged.size();
                while (merged.size() <= index) {
                    merged.add(new Merged());
                }
                Merged target = merged.get(index);
                if (delta.id() != null && target.id == null) {
                    target.id = delta.id();
                }
                if (delta.function() != null) {
                    if (delta.function().name() != null && !delta.function().name().isEmpty()) {
                        target.name = delta.function().name();
                    }
                    if (delta.function().arguments() != null) {
                        target.arguments.append(delta.function().arguments());
                    }
                }
            }
        }

        boolean isEmpty() {
            return merged.isEmpty();
        }

        ChatCompletionChunk flushInto(ChatCompletionChunk template) {
            List<ToolCall> toolCalls = new ArrayList<>();
            for (int i = 0; i < merged.size(); i++) {
                Merged call = merged.get(i);
                toolCalls.add(new ToolCall(i, call.id, "function",
                        new ChatCompletionFunction(call.name, call.arguments.toString())));
            }
            merged.clear();
            ChunkChoice templateChoice = template.choices().get(0);
            ChatCompletionMessage delta = new ChatCompletionMessage(null, Role.ASSISTANT, null, null, toolCalls,
                    null, null, null, null);
            ChunkChoice choice = new ChunkChoice(templateChoice.finishReason(), templateChoice.index(), delta,
                    templateChoice.logprobs());
            return new ChatCompletionChunk(template.id(), List.of(choice), template.created(), template.model(),
                    template.serviceTier(), template.systemFingerprint(), template.object(), template.usage());
        }

        private static final class Merged {
            private String id;
            private String name = "";
            private final StringBuilder arguments = new StringBuilder();
        }
    }
}
