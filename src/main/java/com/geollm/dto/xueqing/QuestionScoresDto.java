package com.geollm.dto.xueqing;

import lombok.Data;

import java.util.List;

@Data
public class QuestionScoresDto {
    private Integer question_id;
    private List<StudentScoreDto> scores;
}

