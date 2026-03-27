package com.geollm.service;

import org.springframework.web.multipart.MultipartFile;

public interface KbService {
    int importDocx(MultipartFile file, String kbName) throws Exception;
}

