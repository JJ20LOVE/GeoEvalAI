package com.geollm.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamUpdateDto {
    @NotNull
    private Integer exam_id;
    @NotBlank
    private String title;
}

