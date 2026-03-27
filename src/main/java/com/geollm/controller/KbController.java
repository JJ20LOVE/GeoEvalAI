package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.service.KbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/kb")
public class KbController {

    private final KbService kbService;

    public KbController(KbService kbService) {
        this.kbService = kbService;
    }

    @PostMapping(value = "/importDocx", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Object>> importDocx(@RequestPart("file") MultipartFile file,
                                                       @RequestParam(value = "kb_name", required = false) String kbName) {
        long t0 = System.currentTimeMillis();
        log.info("POST /kb/importDocx kb_name={} file={} size={}", kbName,
                file == null ? null : file.getOriginalFilename(),
                file == null ? 0 : file.getSize());
        try {
            int added = kbService.importDocx(file, kbName);
            log.info("POST /kb/importDocx done added={} costMs={}", added, System.currentTimeMillis() - t0);
            return ApiResponse.ok(Map.of("added", added));
        } catch (Exception e) {
            log.warn("POST /kb/importDocx failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}

