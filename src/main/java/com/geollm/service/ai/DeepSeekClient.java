package com.geollm.service.ai;

public interface DeepSeekClient {
    String chatCompletion(String model, String prompt, int maxTokens, double temperature);
}

