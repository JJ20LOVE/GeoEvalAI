package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.wrongbook.WrongQuestionRequestDto;
import com.geollm.entity.Wrongbook;
import com.geollm.service.WrongbookService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/wrongbook")
public class WrongbookController {
    private final WrongbookService wrongbookService;

    public WrongbookController(WrongbookService wrongbookService) {
        this.wrongbookService = wrongbookService;
    }

    @PostMapping("/addWrongQuestion")
    public ApiResponse<Void> addWrongQuestion(@Valid @RequestBody WrongQuestionRequestDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("POST /wrongbook/addWrongQuestion student_id={} exam_id={} question_id={}",
                dto.getStudent_id(), dto.getExam_id(), dto.getQuestion_id());
        // 写错题本接口：由 evaluator 或前端手工触发
        int code = wrongbookService.addOrUpdate(dto);
        log.info("POST /wrongbook/addWrongQuestion done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }

    @GetMapping("/getByStudent")
    public ApiResponse<List<Wrongbook>> getByStudent(@RequestParam("student_id") Integer studentId,
                                                           @RequestParam(value = "knowledge_point", required = false) String kp) {
        long t0 = System.currentTimeMillis();
        log.info("GET /wrongbook/getByStudent student_id={} knowledge_point={}", studentId, kp);
        // knowledge_point 为空：返回该学生全部错题；不为空则按知识点筛选
        var data = wrongbookService.listByStudent(studentId, kp);
        log.info("GET /wrongbook/getByStudent done size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/getById")
    public ApiResponse<Wrongbook> getById(@RequestParam("wrong_id") Integer wrongId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /wrongbook/getById wrong_id={}", wrongId);
        Wrongbook w = wrongbookService.getById(wrongId);
        log.info("GET /wrongbook/getById done found={} costMs={}", w != null, System.currentTimeMillis() - t0);
        if (w == null) return ApiResponse.error(606, null);
        return ApiResponse.ok(w);
    }

    @DeleteMapping("/deleteWrongQuestion")
    public ApiResponse<Void> deleteWrongQuestion(@RequestParam("wrong_id") Integer wrongId) {
        long t0 = System.currentTimeMillis();
        log.info("DELETE /wrongbook/deleteWrongQuestion wrong_id={}", wrongId);
        // 删除错题本条目
        int code = wrongbookService.delete(wrongId);
        log.info("DELETE /wrongbook/deleteWrongQuestion done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, null);
        return ApiResponse.ok(null);
    }
}

