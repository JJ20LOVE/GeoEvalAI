package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.exam.ExamUpdateDto;
import com.geollm.dto.exam.YiTuoDto;
import com.geollm.entity.Exam;
import com.geollm.service.ExamService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/exam")
public class ExamController {
    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping(value = "/addExam", consumes = "multipart/form-data")
    public ApiResponse<Integer> addExam(@RequestParam("title") String title,
                                        @RequestParam("creater") Integer creater,
                                        @RequestParam("qnumber") Integer qnumber,
                                        @RequestParam("type") Integer type,
                                        @RequestPart("question") MultipartFile question,
                                        @RequestPart("answer") MultipartFile answer) {
        long t0 = System.currentTimeMillis();
        log.info("POST /exam/addExam title={} creater={} qnumber={} type={}", title, creater, qnumber, type);
        try {
            Integer id = examService.addExam(title, creater, qnumber, type, question, answer);
            if (id == null) return ApiResponse.error(310, null);
            if (id == -604) return ApiResponse.error(604, null);
            log.info("POST /exam/addExam done examId={} costMs={}", id, System.currentTimeMillis() - t0);
            return ApiResponse.ok(id);
        } catch (Exception e) {
            log.warn("POST /exam/addExam failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/getAllExam")
    public ApiResponse<List<Exam>> getAllExam() {
        long t0 = System.currentTimeMillis();
        var data = examService.getAll();
        log.info("GET /exam/getAllExam size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @PutMapping("/updateExam")
    public ApiResponse<Void> updateExam(@Valid @RequestBody ExamUpdateDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("PUT /exam/updateExam exam_id={} title={}", dto.getExam_id(), dto.getTitle());
        int code = examService.updateTitle(dto.getExam_id(), dto.getTitle());
        log.info("PUT /exam/updateExam done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/deleteExam")
    public ApiResponse<Void> deleteExam(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("DELETE /exam/deleteExam id={}", id);
        int code = examService.delete(id);
        log.info("DELETE /exam/deleteExam done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/deUploader")
    public ApiResponse<Void> deUploader(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("DELETE /exam/deUploader id={}", id);
        int code = examService.deUploader(id);
        log.info("DELETE /exam/deUploader done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @PutMapping("/yituo")
    public ApiResponse<Void> yituo(@RequestBody YiTuoDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("PUT /exam/yituo exam_id={}", dto.getExam_id());
        try {
            int code = examService.yituo(dto);
            if (code != 200) return ApiResponse.error(code, null);
            log.info("PUT /exam/yituo done code={} costMs={}", code, System.currentTimeMillis() - t0);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            log.warn("PUT /exam/yituo failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/getExamDetail")
    public ApiResponse<YiTuoDto> getExamDetail(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("GET /exam/getExamDetail id={}", id);
        try {
            YiTuoDto dto = examService.getExamDetail(id);
            if (dto == null) return ApiResponse.error(605, null);
            log.info("GET /exam/getExamDetail done costMs={}", System.currentTimeMillis() - t0);
            return ApiResponse.ok(dto);
        } catch (Exception e) {
            log.warn("GET /exam/getExamDetail failed costMs={} err={}", System.currentTimeMillis() - t0, e.toString());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}

