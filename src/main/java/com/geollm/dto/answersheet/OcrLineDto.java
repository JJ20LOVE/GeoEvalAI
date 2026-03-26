package com.geollm.dto.answersheet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 与接口文档 createAnswerSheet 成功响应中 data 数组项一致：id（小题序号）、result（OCR 文本）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrLineDto {
    private Integer id;
    private String result;
}
