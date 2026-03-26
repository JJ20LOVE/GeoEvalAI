package com.geollm.utils.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class AliyunOcrClient {
    private final WebClient client;
    private final String appCode;
    private final String apiPath;
    private final ObjectMapper om = new ObjectMapper();

    public AliyunOcrClient(String host, String path, String appCode, Duration timeout) {
        this.appCode = appCode;
        this.apiPath = path;
        this.client = WebClient.builder()
                .baseUrl("https://" + host)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "APPCODE " + appCode)
                .build();
    }

    public String ocr(byte[] imageBytes, int timeoutSeconds) throws Exception {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        Map<String, Object> body = Map.of(
                "img", b64,
                "url", "",
                "prob", true,
                "charInfo", false,
                "rotate", true,
                "table", false
        );

        String resp = client.post()
                .uri(apiPath)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
        if (resp == null) return "";

        JsonNode node = om.readTree(resp);
        if (node.hasNonNull("content")) return node.get("content").asText("");
        if (node.hasNonNull("words")) return node.get("words").asText("");
        if (node.hasNonNull("text")) return node.get("text").asText("");
        return "";
    }
}

