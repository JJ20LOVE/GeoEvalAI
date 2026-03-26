package com.geollm.dto.xueqing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StudentInfoDto {
    private Integer id;
    private String student_id;
    private String student_name;
    private String class_name;
    private Double avg_grade;
    private Double max_grade;
    private Double min_grade;

    /** 与 geollm接口文档.yaml 中 getStudentInfo 示例字段名 History 一致 */
    @JsonProperty("History")
    private List<HistoryDto> history;
}

