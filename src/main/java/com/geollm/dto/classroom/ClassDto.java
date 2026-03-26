package com.geollm.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassDto {
    @NotNull
    private Integer class_id;
    @NotBlank
    private String class_name;
}

