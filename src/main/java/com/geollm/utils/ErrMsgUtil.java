package com.geollm.utils;

import java.util.Map;

public class ErrMsgUtil {
    private static final Map<Integer, String> CODE_MSG = Map.ofEntries(
            Map.entry(200, "success"),
            Map.entry(201, "bind failed"),
            Map.entry(203, "upload file failed"),
            Map.entry(204, ""),
            Map.entry(205, "extract file failed"),
            Map.entry(206, "file not found"),
            Map.entry(207, "json error"),
            Map.entry(300, "validation failed"),
            Map.entry(301, "user already exists"),
            Map.entry(310, "user not found"),
            Map.entry(311, "password error"),
            Map.entry(320, "token not found"),
            Map.entry(321, "token invalid"),
            Map.entry(322, "token generate failed"),
            Map.entry(400, "db error"),
            Map.entry(500, "student not found"),
            Map.entry(501, "student already exists"),
            Map.entry(502, "class not found"),
            Map.entry(503, "class already exists"),
            Map.entry(504, "you can't delete a class with students"),
            Map.entry(505, "you can't delete a student with answer sheets"),
            Map.entry(600, "answersheet number error"),
            Map.entry(601, "answer sheet already exists"),
            Map.entry(602, "evaluation interface error"),
            Map.entry(603, "upload answer failed"),
            Map.entry(604, "exam already exists"),
            Map.entry(605, "exam not found"),
            Map.entry(606, "answersheet not found"),
            Map.entry(607, "you can't delete a exam with answer sheets"),
            Map.entry(608, "ocr timeout"),
            Map.entry(609, "delete answersheet failed")
    );

    public static String get(int code) {
        return CODE_MSG.getOrDefault(code, "未知错误");
    }
}

