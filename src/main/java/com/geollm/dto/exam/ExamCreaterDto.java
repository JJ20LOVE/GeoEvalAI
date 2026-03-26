package com.geollm.dto.exam;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ExamCreaterDto {
    private String title;
    private Integer creater;
    private Integer qnumber;
    private Integer type;
    private MultipartFile question;
    private MultipartFile answer;
}

