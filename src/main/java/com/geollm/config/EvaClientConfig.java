package com.geollm.config;

import com.geollm.service.eva.EvaClient;
import com.geollm.service.serviceimpl.eva.EvaClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaClientConfig {
    @Bean
    public EvaClient evaClient(@Value("${eva.baseUrl}") String baseUrl) {
        return new EvaClientImpl(baseUrl);
    }
}

