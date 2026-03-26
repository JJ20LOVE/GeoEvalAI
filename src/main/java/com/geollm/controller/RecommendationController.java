package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.recommendation.RecommendationFeedbackDto;
import com.geollm.dto.recommendation.SimilarQuestionDto;
import com.geollm.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/recommendation")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/getSimilarQuestions")
    public ApiResponse<List<SimilarQuestionDto>> getSimilarQuestions(@RequestParam("wrong_id") Integer wrongId,
                                                                     @RequestParam(value = "limit", required = false) Integer limit) {
        long t0 = System.currentTimeMillis();
        log.info("GET /recommendation/getSimilarQuestions wrong_id={} limit={}", wrongId, limit);
        if (wrongId == null || wrongId == 0) return ApiResponse.error(300, "wrong_id is required");
        int lim = (limit == null || limit <= 0) ? 5 : limit;
        List<SimilarQuestionDto> list = recommendationService.getSimilarQuestions(wrongId, lim);
        log.info("GET /recommendation/getSimilarQuestions done ok={} size={} costMs={}",
                list != null, list == null ? 0 : list.size(), System.currentTimeMillis() - t0);
        if (list == null) return ApiResponse.error(606, null);
        return ApiResponse.ok(list);
    }

    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(@Valid @RequestBody RecommendationFeedbackDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("POST /recommendation/feedback student_id={} wrong_id={} question_id={}",
                dto.getStudent_id(), dto.getWrong_id(), dto.getQuestion_id());
        int code = recommendationService.addFeedback(dto);
        log.info("POST /recommendation/feedback done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }
}

