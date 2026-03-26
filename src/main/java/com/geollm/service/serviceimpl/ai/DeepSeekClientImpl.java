package com.geollm.service.serviceimpl.ai;

import com.geollm.service.ai.DeepSeekClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class DeepSeekClientImpl implements DeepSeekClient {
    private final WebClient client;

    public DeepSeekClientImpl(
            @Value("${ai.deepseek.baseUrl}") String baseUrl,
            @Value("${ai.deepseek.apiKey}") String apiKey
    ) {
        WebClient.Builder b = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.client = b.build();
    }

    @Override
    public String chatCompletion(String model, String prompt, int maxTokens, double temperature) {
        // 构造 DeepSeek/OpenAI 兼容的 chat/completions 请求体
        AIRequest req = new AIRequest();
        req.setModel(model);
        req.setMessages(List.of(new Message("user", prompt)));
        req.setMaxTokens(maxTokens);
        req.setTemperature(temperature);
        req.setStream(false);

        AIResponse resp = client.post()
                .uri("/chat/completions")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(AIResponse.class)
                .block();
        // choices[0].message.content 作为纯文本输出
        if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) return null;
        return resp.getChoices().get(0).getMessage().getContent();
    }

    @Data
    public static class AIRequest {
        private String model;
        private List<Message> messages;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;
        private Boolean stream;
    }

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AIResponse {
        private List<Choice> choices;
        private AIError error;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AIError {
        private String message;
        private String type;
        private String code;
    }
}

