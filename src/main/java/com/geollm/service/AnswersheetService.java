package com.geollm.service;

import com.geollm.dto.answersheet.AnswerSheetInfoDto;
import com.geollm.dto.answersheet.CreateAnswerSheetResult;
import com.geollm.dto.answersheet.OcrEditorDto;
import com.geollm.mapper.AnswersheetMapper;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AnswersheetService {
    List<AnswersheetMapper.RowAnswerSheetWithStudentId> list(String examId, String classId);

    /** 与接口文档 getAnswerSheet 的 data 数组项结构一致（扁平字段） */
    List<Map<String, Object>> listAnswerSheetsForDoc(String examId, String classId) throws Exception;

    CreateAnswerSheetResult create(String studentIdStr, Integer examId, List<MultipartFile> files) throws Exception;

    int correctOcr(OcrEditorDto dto);

    String firstThumbnailUrl(Integer aid, int pageCount) throws Exception;

    AnswerSheetInfoDto getInfo(Integer aid) throws Exception;

    int delete(Integer aid);

    int evaluator(Integer aid);

    List<Map<String, Object>> batchEvaluator(String examId, String classId, int isSkip) throws Exception;
}

