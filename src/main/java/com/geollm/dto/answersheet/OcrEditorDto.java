package com.geollm.dto.answersheet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OcrEditorDto {
    @NotNull
    private Integer aid;
    @NotNull
    private Integer qid;
    private String result;
}

