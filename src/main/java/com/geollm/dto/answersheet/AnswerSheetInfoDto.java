package com.geollm.dto.answersheet;

import lombok.Data;

import java.util.List;

@Data
public class AnswerSheetInfoDto {
    private BasicInfo basic_info;
    private List<QuestionInfo> questions;
    private List<String> pic_urls;

    @Data
    public static class BasicInfo {
        private Integer id;
        private Integer student_id; // answersheet.student_id（数据库是 student 表主键）
        private Integer exam_id;
        private Integer total_grade;
        private Boolean is_eva;
        private String student_name;
    }

    @Data
    public static class QuestionInfo {
        private Integer question_id;
        private String ocr_result;
        private Integer point;
        private String comment;
    }
}

