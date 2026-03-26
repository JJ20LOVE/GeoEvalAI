package com.geollm.dto.wrongbook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WrongQuestionRequestDto {
    @NotNull
    private Integer student_id;
    @NotNull
    private Integer exam_id;
    @NotNull
    private Integer question_id;

    @NotBlank
    private String question_text;
    @NotBlank
    private String student_answer;
    @NotBlank
    private String correct_answer;

    private String analysis;
    private String knowledge_point;
}

