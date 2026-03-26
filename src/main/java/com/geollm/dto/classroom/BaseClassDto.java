package com.geollm.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BaseClassDto {
    @NotBlank
    private String class_name;
}

