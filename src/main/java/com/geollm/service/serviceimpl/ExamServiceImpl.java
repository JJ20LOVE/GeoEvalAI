package com.geollm.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geollm.dto.exam.YiTuoDto;
import com.geollm.entity.*;
import com.geollm.mapper.*;
import com.geollm.service.ExamService;
import com.geollm.utils.docx.DocxExtractor;
import com.geollm.utils.docx.Question;
import com.geollm.utils.docx.Section;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {
    private final ExamMapper examMapper;
    private final ExamDetailMapper examDetailMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final UserMapper userMapper;
    private final AnswersheetMapper answersheetMapper;
    private final ObjectMapper om = new ObjectMapper();

    public ExamServiceImpl(ExamMapper examMapper,
                             ExamDetailMapper examDetailMapper,
                             ExamQuestionMapper examQuestionMapper,
                             ExamAnswerMapper examAnswerMapper,
                             UserMapper userMapper,
                             AnswersheetMapper answersheetMapper) {
        this.examMapper = examMapper;
        this.examDetailMapper = examDetailMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.userMapper = userMapper;
        this.answersheetMapper = answersheetMapper;
    }

    @Override
    public List<Exam> getAll() {
        return examMapper.selectList(null);
    }

    @Override
    public int updateTitle(Integer examId, String title) {
        // 更新考试标题：如果标题重复（且不是当前记录），返回 604
        Exam e = examMapper.selectById(examId);
        if (e == null) return 605;
        Long cnt = examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getTitle, title));
        if (cnt != null && cnt > 0 && !Objects.equals(e.getTitle(), title)) return 604;
        e.setTitle(title);
        examMapper.updateById(e);
        return 200;
    }

    @Override
    @Transactional
    public int delete(Integer examId) {
        // 删除考试：只有当没有对应 answersheet 时才能删（避免数据孤儿）
        Exam e = examMapper.selectById(examId);
        if (e == null) return 605;
        Integer cnt = answersheetMapper.countByExamId(examId);
        if (cnt != null && cnt > 0) return 607;
        examMapper.deleteById(examId);
        examQuestionMapper.deleteById(examId);
        examAnswerMapper.deleteById(examId);
        examDetailMapper.delete(new LambdaQueryWrapper<ExamDetail>().eq(ExamDetail::getExamId, examId));
        return 200;
    }

    @Override
    @Transactional
    public int deUploader(Integer examId) {
        // 反上传：保留 exam 表记录，但删除 exam_question/answer（前端用于“重新上传题目/答案”）
        Exam e = examMapper.selectById(examId);
        if (e == null) return 605;
        examQuestionMapper.deleteById(examId);
        examAnswerMapper.deleteById(examId);
        return 200;
    }

    @Override
    @Transactional
    public Integer addExam(String title, Integer creater, Integer qnumber, Integer type,
                             MultipartFile questionDocx, MultipartFile answerDocx) throws Exception {
        // 新建考试：
        // - exam 表写入基本信息
        // - exam_detail 初始化每个子题（qid=1..qnumber）点值/tihao
        // - question/answer docx 解析成 JSON sections 并写入 exam_question / exam_answer
        // - 同步 exam_detail.tihao（用于 getExamDetail/前端展示小题号）
        if (userMapper.selectById(creater) == null) return null; // controller 返回 310
        Long cnt = examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getTitle, title));
        if (cnt != null && cnt > 0) return -604;

        Exam e = new Exam();
        e.setTitle(title);
        e.setCreater(creater);
        e.setQnumber(qnumber);
        e.setType(type);
        e.setCreateDate(LocalDateTime.now());
        examMapper.insert(e);
        Integer examId = e.getExamId();

        for (int i = 1; i <= qnumber; i++) {
            ExamDetail d = new ExamDetail();
            d.setExamId(examId);
            d.setQid(i);
            d.setPoint(0);
            d.setTihao("");
            examDetailMapper.insert(d);
        }

        List<Section> qSections = DocxExtractor.extract(questionDocx.getInputStream());
        List<Section> aSections = DocxExtractor.extract(answerDocx.getInputStream());
        String qJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(qSections);
        String aJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(aSections);

        ExamQuestion q = new ExamQuestion();
        q.setExamId(examId);
        q.setQuestion(qJson);
        examQuestionMapper.insert(q);

        ExamAnswer a = new ExamAnswer();
        a.setExamId(examId);
        a.setAnswer(aJson);
        examAnswerMapper.insert(a);

        // 同步：更新 exam_detail.tihao = 小题号（qid = qnumber 递增）
        for (Section s : qSections) {
            if (s.getQuestions() == null) continue;
            for (Question qq : s.getQuestions()) {
                try {
                    // docx 提取得到的 qq.number 形如“(1)/(2)/A/B”这种字符串，这里转成 int 做匹配
                    int qid = Integer.parseInt(qq.getNumber());
                    ExamDetail update = new ExamDetail();
                    update.setPoint(0);
                    update.setTihao(qq.getNumber());
                    examDetailMapper.update(update,
                            new LambdaQueryWrapper<ExamDetail>()
                                    .eq(ExamDetail::getExamId, examId)
                                    .eq(ExamDetail::getQid, qid));
                } catch (Exception ignored) {
                }
            }
        }

        return examId;
    }

    @Override
    public YiTuoDto getExamDetail(Integer examId) throws Exception {
        // 取考试详情：把 exam_detail + exam_question/answer JSON 拼成前端 yiTuo 编辑结构
        Exam e = examMapper.selectById(examId);
        if (e == null) return null;

        List<ExamDetail> details = examDetailMapper.selectList(
                new LambdaQueryWrapper<ExamDetail>().eq(ExamDetail::getExamId, examId)
                        .orderByAsc(ExamDetail::getQid));
        ExamQuestion q = examQuestionMapper.selectById(examId);
        ExamAnswer a = examAnswerMapper.selectById(examId);
        if (q == null || a == null) return null;

        List<Section> qSections = om.readValue(q.getQuestion(), new TypeReference<>() {});
        List<Section> aSections = om.readValue(a.getAnswer(), new TypeReference<>() {});

        List<ExamDetail> flat = details;
        int idx = 0;

        YiTuoDto out = new YiTuoDto();
        out.setExam_id(examId);
        List<YiTuoDto.SectionDto> data = new ArrayList<>();

        for (int i = 0; i < qSections.size(); i++) {
            YiTuoDto.SectionDto sec = new YiTuoDto.SectionDto();
            sec.setTitle(qSections.get(i).getTitle());
            List<YiTuoDto.QuestionDto> qs = new ArrayList<>();

            List<Question> qList = qSections.get(i).getQuestions();
            List<Question> aList = (i < aSections.size()) ? aSections.get(i).getQuestions() : List.of();

            for (int j = 0; qList != null && j < qList.size(); j++) {
                YiTuoDto.QuestionDto qq = new YiTuoDto.QuestionDto();
                ExamDetail d = idx < flat.size() ? flat.get(idx) : null;
                qq.setPoint(d == null ? 0 : d.getPoint());
                qq.setNumber(d == null ? qList.get(j).getNumber() : d.getTihao());
                qq.setContent(qList.get(j).getContent());
                qq.setAnswer(j < aList.size() ? aList.get(j).getContent() : "");
                qs.add(qq);
                idx++;
            }
            sec.setQuestions(qs);
            data.add(sec);
        }
        out.setData(data);
        return out;
    }

    @Override
    @Transactional
    public int yituo(YiTuoDto dto) throws Exception {
        // 题目/答案编辑保存：
        // - 根据前端 dto 重写 exam_question / exam_answer JSON
        // - 同步 exam_detail.tihao（用于 q.getTihao 展示）
        Integer examId = dto.getExam_id();
        if (examMapper.selectById(examId) == null) return 605;

        List<Section> qSections = new ArrayList<>();
        List<Section> aSections = new ArrayList<>();
        int counter = 0;

        for (YiTuoDto.SectionDto sec : dto.getData()) {
            Section qs = new Section();
            qs.setTitle(sec.getTitle());
            qs.setQuestions(new ArrayList<>());
            Section as = new Section();
            as.setTitle(sec.getTitle());
            as.setQuestions(new ArrayList<>());

            for (YiTuoDto.QuestionDto q : sec.getQuestions()) {
                counter++;
                // counter 对应“线性 qid”顺序，用于更新 exam_detail 的小题号
                ExamDetail update = new ExamDetail();
                update.setPoint(q.getPoint() == null ? 0 : q.getPoint());
                update.setTihao(q.getNumber());
                examDetailMapper.update(update,
                        new LambdaQueryWrapper<ExamDetail>()
                                .eq(ExamDetail::getExamId, examId)
                                .eq(ExamDetail::getQid, counter));

                Question q1 = new Question();
                q1.setNumber(q.getNumber());
                q1.setContent(q.getContent());
                qs.getQuestions().add(q1);

                Question a1 = new Question();
                a1.setNumber(q.getNumber());
                a1.setContent(q.getAnswer());
                as.getQuestions().add(a1);
            }
            qSections.add(qs);
            aSections.add(as);
        }

        String qJson = om.writeValueAsString(qSections);
        String aJson = om.writeValueAsString(aSections);

        ExamQuestion q = examQuestionMapper.selectById(examId);
        if (q == null) {
            q = new ExamQuestion();
            q.setExamId(examId);
            q.setQuestion(qJson);
            examQuestionMapper.insert(q);
        } else {
            q.setQuestion(qJson);
            examQuestionMapper.updateById(q);
        }

        ExamAnswer a = examAnswerMapper.selectById(examId);
        if (a == null) {
            a = new ExamAnswer();
            a.setExamId(examId);
            a.setAnswer(aJson);
            examAnswerMapper.insert(a);
        } else {
            a.setAnswer(aJson);
            examAnswerMapper.updateById(a);
        }
        return 200;
    }
}

