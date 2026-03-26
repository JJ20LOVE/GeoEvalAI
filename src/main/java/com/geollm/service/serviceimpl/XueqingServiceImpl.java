package com.geollm.service.serviceimpl;

import com.geollm.dto.xueqing.*;
import com.geollm.mapper.XueqingMapper;
import com.geollm.mapper.XueqingRawMapper;
import com.geollm.service.XueqingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class XueqingServiceImpl implements XueqingService {
    private final XueqingMapper xueqingMapper;
    private final XueqingRawMapper rawMapper;

    public XueqingServiceImpl(XueqingMapper xueqingMapper, XueqingRawMapper rawMapper) {
        this.xueqingMapper = xueqingMapper;
        this.rawMapper = rawMapper;
    }

    @Override
    public StudentInfoDto getStudentInfo(Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("Xueqing.getStudentInfo id={}", id);
        // 学生基础信息 + 历史作答/得分趋势（两个 SQL 结果拼起来）
        StudentInfoDto base = xueqingMapper.getStudentInfoBase(id);
        if (base == null) {
            log.info("Xueqing.getStudentInfo notFound id={} costMs={}", id, System.currentTimeMillis() - t0);
            return null;
        }
        base.setHistory(xueqingMapper.getStudentHistory(id));
        log.info("Xueqing.getStudentInfo done historySize={} costMs={}",
                base.getHistory() == null ? 0 : base.getHistory().size(), System.currentTimeMillis() - t0);
        return base;
    }

    @Override
    public List<QuestionScoresDto> getResultByQuestion(Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("Xueqing.getResultByQuestion examId={}", examId);
        // 按 qid 聚合：同一题（qid）下，把每个学生的得分列表返回
        List<XueqingRawMapper.RowQStudentPoint> rows = rawMapper.listQStudentPoints(examId);
        Map<Integer, List<StudentScoreDto>> map = new LinkedHashMap<>();
        for (var r : rows) {
            map.computeIfAbsent(r.getQid(), k -> new ArrayList<>());
            StudentScoreDto s = new StudentScoreDto();
            s.setStudent_id(r.getStudentId());
            s.setStudent_name(r.getStudentName());
            s.setScore(r.getPoint());
            map.get(r.getQid()).add(s);
        }
        List<QuestionScoresDto> out = new ArrayList<>();
        for (var e : map.entrySet()) {
            // 输出顺序保持与 rows 的第一次出现顺序一致（LinkedHashMap）
            QuestionScoresDto q = new QuestionScoresDto();
            q.setQuestion_id(e.getKey());
            q.setScores(e.getValue());
            out.add(q);
        }
        log.info("Xueqing.getResultByQuestion done questions={} costMs={}", out.size(), System.currentTimeMillis() - t0);
        return out;
    }

    @Override
    public List<StudentScoresDto> getResultByStudent(Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("Xueqing.getResultByStudent examId={}", examId);
        // 按 studentId 聚合：同一学生下，把每个题（qid）的得分列表返回
        List<XueqingRawMapper.RowStudentQPoint> rows = rawMapper.listStudentQPoints(examId);
        Map<Integer, List<QuestionScoreDto>> map = new LinkedHashMap<>();
        for (var r : rows) {
            map.computeIfAbsent(r.getStudentId(), k -> new ArrayList<>());
            QuestionScoreDto qs = new QuestionScoreDto();
            qs.setQuestion_id(r.getQid());
            qs.setScore(r.getPoint());
            map.get(r.getStudentId()).add(qs);
        }
        List<StudentScoresDto> out = new ArrayList<>();
        for (var e : map.entrySet()) {
            StudentScoresDto s = new StudentScoresDto();
            s.setStudent_id(e.getKey());
            s.setScores(e.getValue());
            out.add(s);
        }
        log.info("Xueqing.getResultByStudent done students={} costMs={}", out.size(), System.currentTimeMillis() - t0);
        return out;
    }

    @Override
    public List<Double> getQuestionPointRate(Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("Xueqing.getQuestionPointRate examId={}", examId);
        // 点得分率：avg_point / full_point（逐题计算），full_point 为 0 时返回 0
        List<Double> avg = xueqingMapper.getAvgPointByQid(examId);
        List<Double> full = xueqingMapper.getFullPointByQid(examId);
        if (avg == null || full == null) return List.of();
        int n = Math.min(avg.size(), full.size());
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double denom = full.get(i) == null ? 0 : full.get(i);
            double v = avg.get(i) == null ? 0 : avg.get(i);
            out.add(denom == 0 ? 0 : v / denom);
        }
        log.info("Xueqing.getQuestionPointRate done size={} costMs={}", out.size(), System.currentTimeMillis() - t0);
        return out;
    }

    @Override
    public Map<String, List<StudentListDto>> getNameList(Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("Xueqing.getNameList examId={}", examId);
        // top/back：用“中间落差”截取两端（用于前端展示最优/最弱学生）
        List<StudentListDto> list = xueqingMapper.getNameList(examId);
        if (list == null || list.isEmpty()) return Map.of("top", List.of(), "back", List.of());
        int p = (int) (0.27 * list.size() + 0.5);
        if (p <= 0) p = 1;
        if (p * 2 > list.size()) p = Math.max(1, list.size() / 2);
        var out = Map.of(
                "top", list.subList(0, p),
                "back", list.subList(list.size() - p, list.size())
        );
        log.info("Xueqing.getNameList done top={} back={} costMs={}",
                out.get("top").size(), out.get("back").size(), System.currentTimeMillis() - t0);
        return out;
    }

    @Override
    public ExamDataDto getExamData(Integer examId) {
        long t0 = System.currentTimeMillis();
        log.info("Xueqing.getExamData examId={}", examId);
        ExamDataDto dto = xueqingMapper.getExamData(examId);
        log.info("Xueqing.getExamData done ok={} costMs={}", dto != null, System.currentTimeMillis() - t0);
        return dto;
    }
}

