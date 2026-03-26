package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.answersheet.CreateAnswerSheetResult;
import com.geollm.dto.answersheet.OcrEditorDto;
import com.geollm.dto.answersheet.OcrLineDto;
import com.geollm.service.AnswersheetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/answersheet")
public class AnswersheetController {
    private final AnswersheetService answersheetService;

    public AnswersheetController(AnswersheetService answersheetService) {
        this.answersheetService = answersheetService;
    }

    @GetMapping("/getAnswerSheet")
    public ApiResponse<List<Map<String, Object>>> getAnswerSheet(@RequestParam(value = "exam_id", required = false) String examId,
                                                                   @RequestParam(value = "class_id", required = false) String classId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /answersheet/getAnswerSheet exam_id={} class_id={}", examId, classId);
        try {
            var data = answersheetService.listAnswerSheetsForDoc(examId, classId);
            log.info("GET /answersheet/getAnswerSheet done size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            log.warn("GET /answersheet/getAnswerSheet failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping(value = "/createAnswerSheet", consumes = "multipart/form-data")
    public ApiResponse<List<OcrLineDto>> createAnswerSheet(@RequestParam("student_id") String studentId,
                                                           @RequestParam("exam_id") Integer examId,
                                                           @RequestPart("file") List<MultipartFile> files) {
        long t0 = System.currentTimeMillis();
        log.info("POST /answersheet/createAnswerSheet student_id={} exam_id={} fileCount={}", studentId, examId, files == null ? 0 : files.size());
        try {
            CreateAnswerSheetResult res = answersheetService.create(studentId, examId, files);
            if (res.getCode() == 605) return ApiResponse.error(605, null);
            if (res.getCode() == 500) return ApiResponse.error(500, null);
            if (res.getCode() == 601) return ApiResponse.error(601, null);
            log.info("POST /answersheet/createAnswerSheet done ocrCount={} costMs={}",
                    res.getOcrLines() == null ? 0 : res.getOcrLines().size(),
                    System.currentTimeMillis() - t0);
            return ApiResponse.ok(res.getOcrLines());
        } catch (Exception e) {
            log.warn("POST /answersheet/createAnswerSheet failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/correctOcr")
    public ApiResponse<Void> correctOcr(@Valid @RequestBody OcrEditorDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("PUT /answersheet/correctOcr aid={} qid={}", dto.getAid(), dto.getQid());
        int code = answersheetService.correctOcr(dto);
        log.info("PUT /answersheet/correctOcr done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @GetMapping("/evaluator")
    public ApiResponse<Void> evaluator(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("GET /answersheet/evaluator id={}", id);
        int code = answersheetService.evaluator(id);
        log.info("GET /answersheet/evaluator done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @GetMapping("/getAnswerSheetInfo")
    public ApiResponse<?> getAnswerSheetInfo(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("GET /answersheet/getAnswerSheetInfo id={}", id);
        try {
            var info = answersheetService.getInfo(id);
            if (info == null) return ApiResponse.error(606, null);
            log.info("GET /answersheet/getAnswerSheetInfo done costMs={}", System.currentTimeMillis() - t0);
            return ApiResponse.ok(info);
        } catch (Exception e) {
            log.warn("GET /answersheet/getAnswerSheetInfo failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/deleteAnswerSheet")
    public ApiResponse<Void> deleteAnswerSheet(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("DELETE /answersheet/deleteAnswerSheet id={}", id);
        int code = answersheetService.delete(id);
        log.info("DELETE /answersheet/deleteAnswerSheet done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @GetMapping("/batchEvaluator")
    public ApiResponse<List<Map<String, Object>>> batchEvaluator(@RequestParam(value = "exam_id", required = false) String examId,
                                                                  @RequestParam(value = "class_id", required = false) String classId,
                                                                  @RequestParam("is_skip") int isSkip) {
        long t0 = System.currentTimeMillis();
        log.info("GET /answersheet/batchEvaluator exam_id={} class_id={} is_skip={}", examId, classId, isSkip);
        try {
            var data = answersheetService.batchEvaluator(examId, classId, isSkip);
            log.info("GET /answersheet/batchEvaluator done size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            log.warn("GET /answersheet/batchEvaluator failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
