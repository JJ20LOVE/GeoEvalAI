package com.geollm.dto.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentDto {
    @NotNull
    private Integer id;
    @NotBlank
    private String student_id;
    @NotBlank
    private String student_name;
    @NotNull
    private Integer class_id;
}

