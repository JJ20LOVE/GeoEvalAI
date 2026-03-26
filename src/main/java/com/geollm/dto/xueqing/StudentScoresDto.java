package com.geollm.dto.xueqing;

import lombok.Data;

import java.util.List;

@Data
public class StudentScoresDto {
    private Integer student_id;
    private List<QuestionScoreDto> scores;
}

