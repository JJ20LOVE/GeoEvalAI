package com.geollm.dto.recommendation;

import lombok.Data;

@Data
public class SimilarQuestionDto {
    private Long batch_id;
    private Long item_id;
    private Integer question_id;
    private String question_text;
    private String knowledge_point;
    private String difficulty;
}

