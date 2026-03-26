package com.geollm.dto.recommendation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendationFeedbackDto {
    @NotNull
    private Integer student_id;
    @NotNull
    private Integer wrong_id;
    @NotNull
    private Integer question_id;
    @NotBlank
    private String feedback;
}

