package com.geollm.service.serviceimpl.eva;

import com.geollm.service.eva.EvaClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

public class EvaClientImpl implements EvaClient {
    private final WebClient client;

    public EvaClientImpl(String baseUrl) {
        // 评测服务地址（zer0.top/flask）由配置注入，这里只负责 HTTP 调用封装
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Result eva(String question, String correctAnswer, String studentAnswer, int fullScore, int timeoutSeconds) {
        // 请求 body：与 Go 后端 eva() 语义保持一致
        // question/correct_answer/student_answer/full_score -> /llm_comment 返回 score/comment/structure
        Map<String, Object> body = Map.of(
                "question", question,
                "full_score", fullScore,
                "correct_answer", correctAnswer,
                "student_answer", studentAnswer
        );

        EResponse resp = client.post()
                .uri("/llm_comment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .onErrorReturn(new EResponse())
                .block();

        // 出错/返回空 -> 直接给默认空 Result，避免评分流程中断
        if (resp == null || resp.getError() != null && !resp.getError().isBlank()) return new Result();
        return resp.getResult() == null ? new Result() : resp.getResult();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EResponse {
        private Result result;
        private String error;
        private String detail;

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}

