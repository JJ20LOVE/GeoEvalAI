package com.geollm.dto.answersheet;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * createAnswerSheet：成功时 code=200 且 ocrLines 非空；失败时 errorCode 为业务码，ocrLines 为 null
 */
@Data
@AllArgsConstructor
public class CreateAnswerSheetResult {
    private int code;
    private List<OcrLineDto> ocrLines;

    public static CreateAnswerSheetResult ok(List<OcrLineDto> ocrLines) {
        return new CreateAnswerSheetResult(200, ocrLines);
    }

    public static CreateAnswerSheetResult error(int code) {
        return new CreateAnswerSheetResult(code, null);
    }
}
