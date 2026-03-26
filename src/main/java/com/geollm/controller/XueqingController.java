package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.xueqing.ExamDataDto;
import com.geollm.dto.xueqing.QuestionScoresDto;
import com.geollm.dto.xueqing.StudentInfoDto;
import com.geollm.dto.xueqing.StudentScoresDto;
import com.geollm.dto.xueqing.StudentListDto;
import com.geollm.service.XueqingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/xueqing")
public class XueqingController {
    private final XueqingService xueqingService;

    public XueqingController(XueqingService xueqingService) {
        this.xueqingService = xueqingService;
    }

    @GetMapping("/getResultByQuestion")
    public ApiResponse<List<QuestionScoresDto>> getResultByQuestion(@RequestParam("exam_id") Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /xueqing/getResultByQuestion exam_id={}", examId);
        // 按题目聚合：对同一题，返回所有学生的得分列表
        var data = xueqingService.getResultByQuestion(examId);
        log.info("GET /xueqing/getResultByQuestion done size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/getResultByStudent")
    public ApiResponse<List<StudentScoresDto>> getResultByStudent(@RequestParam("exam_id") Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /xueqing/getResultByStudent exam_id={}", examId);
        // 按学生聚合：对同一学生，返回所有题的得分列表
        var data = xueqingService.getResultByStudent(examId);
        log.info("GET /xueqing/getResultByStudent done size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/getQuestionPointRate")
    public ApiResponse<List<Double>> getQuestionPointRate(@RequestParam("exam_id") Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /xueqing/getQuestionPointRate exam_id={}", examId);
        // 点得分率曲线：avg_point / full_point（逐题返回）
        var data = xueqingService.getQuestionPointRate(examId);
        log.info("GET /xueqing/getQuestionPointRate done size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/getNameList")
    public ApiResponse<Map<String, List<StudentListDto>>> getNameList(@RequestParam("exam_id") Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /xueqing/getNameList exam_id={}", examId);
        // top/back：用于前端展示“强/弱学生”的分组列表
        var data = xueqingService.getNameList(examId);
        log.info("GET /xueqing/getNameList done keys={} costMs={}", data == null ? 0 : data.keySet().size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/getExamData")
    public ApiResponse<ExamDataDto> getExamData(@RequestParam("exam_id") Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("GET /xueqing/getExamData exam_id={}", examId);
        // 试卷概览数据：用于 xueqing 总览页
        var data = xueqingService.getExamData(examId);
        log.info("GET /xueqing/getExamData done ok={} costMs={}", data != null, System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    // SOLO 依赖外部 flask/LLM，在迁移 answersheet 评分后再补齐
    @GetMapping("/solo")
    public ApiResponse<Void> soloPlaceholder() {
        return ApiResponse.error(501, "not implemented yet");
    }
}

