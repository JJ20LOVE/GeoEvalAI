package com.geollm.service.eva;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

public interface EvaClient {
    Result eva(String question, String correctAnswer, String studentAnswer, int fullScore, int timeoutSeconds);

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Integer score = 0;
        private String comment = "";
        private Integer structure = 0;
    }
}

