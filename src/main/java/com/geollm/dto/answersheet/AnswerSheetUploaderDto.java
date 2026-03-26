package com.geollm.dto.answersheet;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AnswerSheetUploaderDto {
    private String student_id;
    private Integer exam_id;
    private List<MultipartFile> file;
}

