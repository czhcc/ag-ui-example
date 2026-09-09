package com.ac.agent.config;

import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Creates an OpenAI-compatible client without an Authorization header.
 *
 * <p>Spring AI's default OpenAI auto-configuration requires a non-empty API key.
 * The configured inference service does not use authentication, so a
 * {@link NoopApiKey} is required instead of a placeholder credential.</p>
 */
@Configuration
public class OpenAiCompatibleConfig {

    @Bean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.completions-path}") String completionsPath,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
            ResponseErrorHandler responseErrorHandler) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new NoopApiKey())
                .completionsPath(completionsPath)
                .embeddingsPath("/embeddings")
                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
                .responseErrorHandler(responseErrorHandler)
                .build();
    }
}
