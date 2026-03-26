package com.geollm.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geollm.dto.wrongbook.WrongQuestionRequestDto;
import com.geollm.entity.Wrongbook;
import com.geollm.mapper.WrongbookMapper;
import com.geollm.service.WrongbookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class WrongbookServiceImpl extends ServiceImpl<WrongbookMapper, Wrongbook> implements WrongbookService {
    private final WrongbookMapper wrongbookMapper;

    public WrongbookServiceImpl(WrongbookMapper wrongbookMapper) {
        this.wrongbookMapper = wrongbookMapper;
    }

    @Override
    public int addOrUpdate(WrongQuestionRequestDto dto) {
        log.debug("Wrongbook.addOrUpdate studentId={} examId={} questionId={}",
                dto.getStudent_id(), dto.getExam_id(), dto.getQuestion_id());
        // 写错题本：同一学生同一考试同一道题，只允许一条记录
        // 查重键：student_id + exam_id + question_id
        Wrongbook existing = wrongbookMapper.selectOne(new LambdaQueryWrapper<Wrongbook>()
                .eq(Wrongbook::getStudentId, dto.getStudent_id())
                .eq(Wrongbook::getExamId, dto.getExam_id())
                .eq(Wrongbook::getQuestionId, dto.getQuestion_id())
                .last("LIMIT 1"));

        // 已存在：覆盖题干/答题/解析/知识点（保证前端展示是最新评语）
        if (existing != null) {
            existing.setQuestionText(dto.getQuestion_text());
            existing.setStudentAnswer(dto.getStudent_answer());
            existing.setCorrectAnswer(dto.getCorrect_answer());
            existing.setAnalysis(dto.getAnalysis());
            existing.setKnowledgePoint(dto.getKnowledge_point());
            wrongbookMapper.updateById(existing);
            log.info("Wrongbook.addOrUpdate updated wrongId={}", existing.getWrongId());
            return 200;
        }

        // 不存在：插入新错题记录
        Wrongbook w = new Wrongbook();
        w.setStudentId(dto.getStudent_id());
        w.setExamId(dto.getExam_id());
        w.setQuestionId(dto.getQuestion_id());
        w.setQuestionText(dto.getQuestion_text());
        w.setStudentAnswer(dto.getStudent_answer());
        w.setCorrectAnswer(dto.getCorrect_answer());
        w.setAnalysis(dto.getAnalysis());
        w.setKnowledgePoint(dto.getKnowledge_point());
        wrongbookMapper.insert(w);
        log.info("Wrongbook.addOrUpdate inserted wrongId={}", w.getWrongId());
        return 200;
    }

    @Override
    public List<Wrongbook> listByStudent(Integer studentId, String knowledgePoint) {
        log.debug("Wrongbook.listByStudent studentId={} knowledgePoint={}", studentId, knowledgePoint);
        // knowledgePoint 为空：返回该学生全部错题
        // knowledgePoint 不为空：返回该知识点下的错题
        LambdaQueryWrapper<Wrongbook> w = new LambdaQueryWrapper<Wrongbook>()
                .eq(Wrongbook::getStudentId, studentId)
                .orderByDesc(Wrongbook::getCreateTime);
        if (knowledgePoint != null && !knowledgePoint.isBlank()) {
            w.eq(Wrongbook::getKnowledgePoint, knowledgePoint);
        }
        var list = wrongbookMapper.selectList(w);
        log.info("Wrongbook.listByStudent done size={}", list == null ? 0 : list.size());
        return list;
    }

    @Override
    public Wrongbook getById(Integer wrongId) {
        log.debug("Wrongbook.getById wrongId={}", wrongId);
        return wrongbookMapper.selectById(wrongId);
    }

    @Override
    public int delete(Integer wrongId) {
        log.info("Wrongbook.delete wrongId={}", wrongId);
        // 删除失败：影响行数为 0，对应 Go 后端错误码 606
        int rows = wrongbookMapper.deleteById(wrongId);
        if (rows == 0) return 606;
        return 200;
    }
}

