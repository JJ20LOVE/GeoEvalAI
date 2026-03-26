package com.geollm.service;

import com.geollm.dto.recommendation.RecommendationFeedbackDto;
import com.geollm.dto.recommendation.SimilarQuestionDto;

import java.util.List;

public interface RecommendationService {
    List<SimilarQuestionDto> getSimilarQuestions(int wrongId, int limit);

    int addFeedback(RecommendationFeedbackDto dto);
}

