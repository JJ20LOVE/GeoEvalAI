package com.geollm.dto.exam;

import lombok.Data;

import java.util.List;

@Data
public class YiTuoDto {
    private Integer exam_id;
    private List<SectionDto> data;

    @Data
    public static class SectionDto {
        private String Title;
        private List<QuestionDto> Questions;
    }

    @Data
    public static class QuestionDto {
        private Integer Point;
        private String Number;
        private String Content;
        private String Answer;
    }
}

