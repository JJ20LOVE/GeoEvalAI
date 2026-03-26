package com.geollm.dto.recommendation;

import lombok.Data;

@Data
public class SimilarQuestionDto {
    private Integer question_id;
    private String question_text;
    private String knowledge_point;
    private String difficulty;
}

