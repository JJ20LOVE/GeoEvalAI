package com.geollm.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePassParam {
    @NotBlank
    private String username;
    @NotBlank
    private String old_pass;
    @NotBlank
    private String new_pass;
}

