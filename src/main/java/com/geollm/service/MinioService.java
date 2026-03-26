package com.geollm.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

public interface MinioService {
    void upload(String objectName, MultipartFile file) throws Exception;

    String presignedGetUrl(String objectName, Duration expiry) throws Exception;

    void delete(String objectName) throws Exception;
}

