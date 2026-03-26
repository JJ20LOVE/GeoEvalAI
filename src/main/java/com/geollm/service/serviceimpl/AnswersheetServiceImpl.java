package com.geollm.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geollm.dto.answersheet.AnswerSheetInfoDto;
import com.geollm.dto.answersheet.CreateAnswerSheetResult;
import com.geollm.dto.answersheet.OcrLineDto;
import com.geollm.dto.answersheet.OcrEditorDto;
import com.geollm.entity.*;
import com.geollm.mapper.*;
import com.geollm.service.AnswersheetService;
import com.geollm.service.MinioService;
import com.geollm.service.WrongbookService;
import com.geollm.service.eva.EvaClient;
import com.geollm.service.ocr.TemplateOcrService;
import com.geollm.utils.docx.Question;
import com.geollm.utils.docx.Section;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnswersheetServiceImpl extends ServiceImpl<AnswersheetMapper, Answersheet> implements AnswersheetService {

    private final AnswersheetMapper answersheetMapper;
    private final StudentMapper studentMapper;
    private final ExamMapper examMapper;
    private final AnswersheetDetailMapper detailMapper;
    private final OcrMapper ocrMapper;
    private final AnswersheetInfoMapper infoMapper;
    private final MinioService minioService;
    private final TemplateOcrService templateOcrService;
    private final WrongbookService wrongbookService;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ObjectMapper om = new ObjectMapper();
    private final EvaClient evaClient;
    private final int evaTimeoutSeconds;

    public AnswersheetServiceImpl(AnswersheetMapper answersheetMapper,
                                   StudentMapper studentMapper,
                                   ExamMapper examMapper,
                                   AnswersheetDetailMapper detailMapper,
                                   OcrMapper ocrMapper,
                                   AnswersheetInfoMapper infoMapper,
                                   MinioService minioService,
                                   TemplateOcrService templateOcrService,
                                   WrongbookService wrongbookService,
                                   ExamQuestionMapper examQuestionMapper,
                                   ExamAnswerMapper examAnswerMapper,
                                   EvaClient evaClient,
                                   @Value("${eva.timeoutSeconds}") int evaTimeoutSeconds) {
        this.answersheetMapper = answersheetMapper;
        this.studentMapper = studentMapper;
        this.examMapper = examMapper;
        this.detailMapper = detailMapper;
        this.ocrMapper = ocrMapper;
        this.infoMapper = infoMapper;
        this.minioService = minioService;
        this.templateOcrService = templateOcrService;
        this.wrongbookService = wrongbookService;
        this.examQuestionMapper = examQuestionMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.evaClient = evaClient;
        this.evaTimeoutSeconds = evaTimeoutSeconds;
    }

    @Override
    public List<AnswersheetMapper.RowAnswerSheetWithStudentId> list(String examId, String classId) {
        return answersheetMapper.listAnswerSheets(examId == null ? "" : examId, classId == null ? "" : classId);
    }

    @Override
    public List<Map<String, Object>> listAnswerSheetsForDoc(String examId, String classId) throws Exception {
        long t0 = System.currentTimeMillis();
        log.info("Answersheet.listAnswerSheetsForDoc examId={} classId={}", examId, classId);
        List<AnswersheetMapper.RowAnswerSheetWithStudentId> rows =
                list(examId, classId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (var r : rows) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", r.getId());
            row.put("student_id", studentIdForApi(r.getStudentId()));
            row.put("exam_id", r.getExamId());
            row.put("total_grade", r.getTotalGrade());
            row.put("is_eva", r.getIsEva());
            String picUrl;
            try {
                picUrl = firstThumbnailUrl(r.getId(), 1);
            } catch (Exception e) {
                picUrl = "";
            }
            row.put("pic_url", picUrl == null ? "" : picUrl);
            out.add(row);
        }
        log.info("Answersheet.listAnswerSheetsForDoc done size={} costMs={}", out.size(), System.currentTimeMillis() - t0);
        return out;
    }

    @Override
    @Transactional
    public CreateAnswerSheetResult create(String studentIdStr, Integer examId, List<MultipartFile> files) throws Exception {
        long t0 = System.currentTimeMillis();
        log.info("Answersheet.create studentId={} examId={} fileCount={}", studentIdStr, examId, files == null ? 0 : files.size());
        // 答题纸上传入口：创建 answersheet + 初始化明细 -> 上传图片 -> 模板OCR识别 -> 写入Ocr表
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            log.info("Answersheet.create failed: exam not found examId={}", examId);
            return CreateAnswerSheetResult.error(605);
        }

        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentId, studentIdStr)
                .last("LIMIT 1"));
        if (student == null) {
            log.info("Answersheet.create failed: student not found studentId={}", studentIdStr);
            return CreateAnswerSheetResult.error(500);
        }

        // 601：同一学生同一考试只能一份
        Long cnt = answersheetMapper.selectCount(new LambdaQueryWrapper<Answersheet>()
                .eq(Answersheet::getStudentId, student.getId())
                .eq(Answersheet::getExamId, examId));
        if (cnt != null && cnt > 0) {
            log.info("Answersheet.create rejected: duplicate studentId={} examId={}", studentIdStr, examId);
            return CreateAnswerSheetResult.error(601);
        }

        Answersheet as = new Answersheet();
        as.setStudentId(student.getId());
        as.setExamId(examId);
        as.setTotalGrade(0);
        as.setIsEva(false);
        answersheetMapper.insert(as);
        Integer aid = as.getId();
        log.info("Answersheet.create inserted answersheetId={}", aid);

        // 初始化 answersheet_detail：每个子题（q1..qn）先占位，后续 evaluator 才写分数/评语
        for (int i = 1; i <= exam.getQnumber(); i++) {
            AnswersheetDetail d = new AnswersheetDetail();
            d.setAid(aid);
            d.setQid(i);
            d.setPoint(0);
            d.setComment("");
            d.setStructure(0);
            detailMapper.insert(d);
        }
        log.debug("Answersheet.create initialized details answersheetId={} qnumber={}", aid, exam.getQnumber());

        // 上传图片到 MinIO：objectName 按 Go 后端兼容命名 -> `${aid}_${pageIndex}`
        List<byte[]> pageBytes = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            pageBytes.add(files.get(i).getBytes());
            minioService.upload(aid + "_" + i, files.get(i));
            // 临时：先把缩略图直接存同一张（后续可再做压缩生成）
            minioService.upload(aid + "_" + i + "_thumbnail", files.get(i));
        }
        log.info("Answersheet.create uploaded pages answersheetId={} pages={}", aid, files == null ? 0 : files.size());

        // OCR：优先走模板 ROI 切题；没有 ROI 模板就写入空字符串（让前端可手工修正）
        var ocrMap = templateOcrService.ocrByTemplate(examId, pageBytes, exam.getQnumber());
        List<OcrLineDto> ocrLines = new ArrayList<>();
        for (int i = 1; i <= exam.getQnumber(); i++) {
            String text = ocrMap == null ? "" : ocrMap.getOrDefault(i, "");
            Ocr o = new Ocr();
            o.setAid(aid);
            o.setQid(i);
            o.setResult(text);
            ocrMapper.insert(o);
            ocrLines.add(new OcrLineDto(i, text));
        }
        log.info("Answersheet.create done answersheetId={} ocrLines={} costMs={}", aid, ocrLines.size(), System.currentTimeMillis() - t0);
        return CreateAnswerSheetResult.ok(ocrLines);
    }

    @Override
    public int correctOcr(OcrEditorDto dto) {
        log.info("Answersheet.correctOcr aid={} qid={} textLen={}", dto.getAid(), dto.getQid(),
                dto.getResult() == null ? 0 : dto.getResult().length());
        // 前端手动修正 OCR：只更新对应 (aid, qid) 的结果文本
        Ocr o = ocrMapper.selectOne(new LambdaQueryWrapper<Ocr>()
                .eq(Ocr::getAid, dto.getAid())
                .eq(Ocr::getQid, dto.getQid())
                .last("LIMIT 1"));
        if (o == null) return 606;
        o.setResult(dto.getResult());
        ocrMapper.updateById(o);
        return 200;
    }

    @Override
    public String firstThumbnailUrl(Integer aid, int pageCount) throws Exception {
        // 取第一张题目图的缩略图（用于列表预览）
        if (pageCount <= 0) return null;
        return minioService.presignedGetUrl(aid + "_0_thumbnail", Duration.ofDays(1));
    }

    @Override
    public AnswerSheetInfoDto getInfo(Integer aid) throws Exception {
        long t0 = System.currentTimeMillis();
        log.info("Answersheet.getInfo aid={}", aid);
        var basic = answersheetMapper.getBasicInfo(aid);
        if (basic == null) return null;

        Exam exam = examMapper.selectById(basic.getExamId());
        if (exam == null) return null;

        AnswerSheetInfoDto dto = new AnswerSheetInfoDto();
        AnswerSheetInfoDto.BasicInfo bi = new AnswerSheetInfoDto.BasicInfo();
        bi.setId(basic.getId());
        bi.setStudent_id(basic.getStudentId());
        bi.setExam_id(basic.getExamId());
        bi.setTotal_grade(basic.getTotalGrade());
        bi.setIs_eva(basic.getIsEva());
        bi.setStudent_name(basic.getStudentName());
        dto.setBasic_info(bi);

        dto.setQuestions(infoMapper.listQuestions(aid));

        // Go 后端语义：exam.type 用于表示“答案页数量”，页面总数 = type + 1（首页/封面）
        int pageCount = (exam.getType() == null ? 0 : exam.getType()) + 1;
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            urls.add(minioService.presignedGetUrl(aid + "_" + i, Duration.ofDays(1)));
        }
        dto.setPic_urls(urls);
        log.info("Answersheet.getInfo done aid={} qCount={} pageCount={} costMs={}",
                aid,
                dto.getQuestions() == null ? 0 : dto.getQuestions().size(),
                urls.size(),
                System.currentTimeMillis() - t0);
        return dto;
    }

    @Override
    @Transactional
    public int delete(Integer aid) {
        // 删除入口：先删库表，再删 MinIO 对象（图片 + 缩略图）
        Answersheet as = answersheetMapper.selectById(aid);
        if (as == null) return 606;

        Exam exam = examMapper.selectById(as.getExamId());
        int pageCount = (exam != null && exam.getType() != null ? exam.getType() : 0) + 1;

        answersheetMapper.deleteById(aid);
        ocrMapper.delete(new LambdaQueryWrapper<Ocr>().eq(Ocr::getAid, aid));
        detailMapper.delete(new LambdaQueryWrapper<AnswersheetDetail>().eq(AnswersheetDetail::getAid, aid));

        for (int i = 0; i < pageCount; i++) {
            try {
                minioService.delete(aid + "_" + i);
            } catch (Exception ignored) {
            }
            try {
                minioService.delete(aid + "_" + i + "_thumbnail");
            } catch (Exception ignored) {
            }
        }
        return 200;
    }

    @Override
    @Transactional
    public int evaluator(Integer aid) {
        // 评分入口：读取试题/标准答案 JSON -> 按 section/qid 逐题 Eva 打分 -> 更新 answersheet_detail
        Answersheet as = answersheetMapper.selectById(aid);
        if (as == null) return 606;

        Exam exam = examMapper.selectById(as.getExamId());
        if (exam == null) return 605;

        var q = examQuestionMapper.selectById(exam.getExamId());
        var a = examAnswerMapper.selectById(exam.getExamId());
        if (q == null || a == null) return 400;

        List<Section> qSections;
        List<Section> aSections;
        try {
            // question/answer 字段在库里以 JSON 存储，这里反序列化成 Section/Question 结构
            qSections = om.readValue(q.getQuestion(), new TypeReference<>() {});
            aSections = om.readValue(a.getAnswer(), new TypeReference<>() {});
        } catch (Exception e) {
            return 207;
        }

        List<AnswersheetDetail> details = detailMapper.selectList(new LambdaQueryWrapper<AnswersheetDetail>()
                .eq(AnswersheetDetail::getAid, aid)
                .orderByAsc(AnswersheetDetail::getQid));
        List<Ocr> ocrs = ocrMapper.selectList(new LambdaQueryWrapper<Ocr>()
                .eq(Ocr::getAid, aid)
                .orderByAsc(Ocr::getQid));

        int total = 0;
        // idx：answersheet_detail / ocrs 的“线性索引”，用于把 Section 内问题映射到对应子题行
        int idx = 0;

        for (int si = 0; si < aSections.size(); si++) {
            List<Question> ansQs = aSections.get(si).getQuestions();
            List<Question> quesQs = si < qSections.size() ? qSections.get(si).getQuestions() : List.of();
            String title = si < qSections.size() ? qSections.get(si).getTitle() : "";
            if (ansQs == null) continue;

            for (int qj = 0; qj < ansQs.size(); qj++) {
                if (idx >= details.size()) break;
                int full = details.get(idx).getPoint() == null ? 0 : details.get(idx).getPoint();
                String studentAns = idx < ocrs.size()
                        ? (ocrs.get(idx).getResult() == null ? "" : ocrs.get(idx).getResult())
                        : "";
                String correct = ansQs.get(qj).getContent();
                String questionText = title + (qj < quesQs.size() ? quesQs.get(qj).getContent() : "");

                EvaClient.Result r = evaClient.eva(questionText, correct, studentAns, full, evaTimeoutSeconds);

                AnswersheetDetail d = details.get(idx);
                d.setPoint(r.getScore());
                d.setComment(r.getComment());
                d.setStructure(r.getStructure());
                detailMapper.updateById(d);
                total += r.getScore() == null ? 0 : r.getScore();

                // 只要没满分，就把该题加入错题本（knowledge_point 留空，后续可再做抽取/补全）
                if ((r.getScore() == null ? 0 : r.getScore()) < full) {
                    // 自动加入错题本：知识点先为空（后续可补 knowledge_point 提取）
                    com.geollm.dto.wrongbook.WrongQuestionRequestDto wq = new com.geollm.dto.wrongbook.WrongQuestionRequestDto();
                    wq.setStudent_id(as.getStudentId());
                    wq.setExam_id(as.getExamId());
                    wq.setQuestion_id(idx + 1);
                    wq.setQuestion_text(qj < quesQs.size() ? quesQs.get(qj).getContent() : "");
                    wq.setStudent_answer(studentAns);
                    wq.setCorrect_answer(correct);
                    wq.setAnalysis(r.getComment());
                    wq.setKnowledge_point("");
                    wrongbookService.addOrUpdate(wq);
                }

                idx++;
            }
        }

        as.setTotalGrade(total);
        as.setIsEva(true);
        answersheetMapper.updateById(as);
        return 200;
    }

    @Override
    public List<Map<String, Object>> batchEvaluator(String examId, String classId, int isSkip) throws Exception {
        // 批量评分：与 Go 语义一致 -> 先查出集合，再逐个调用 evaluator；响应体与接口文档 batchEvaluator 一致
        List<AnswersheetMapper.RowAnswerSheetWithStudentId> list =
                answersheetMapper.listAnswerSheets(examId == null ? "" : examId, classId == null ? "" : classId);
        for (var r : list) {
            if (isSkip == 1) {
                Answersheet as = answersheetMapper.selectById(r.getId());
                if (as != null && Boolean.TRUE.equals(as.getIsEva())) continue;
            }
            evaluator(r.getId());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (var r : list) {
            Answersheet as = answersheetMapper.selectById(r.getId());
            Map<String, Object> item = new HashMap<>();
            Map<String, Object> basic = new HashMap<>();
            basic.put("id", r.getId());
            basic.put("student_id", studentIdForApi(r.getStudentId()));
            basic.put("exam_id", r.getExamId());
            basic.put("total_grade", as != null ? as.getTotalGrade() : r.getTotalGrade());
            basic.put("is_eva", as != null && as.getIsEva() != null ? as.getIsEva() : Boolean.FALSE);
            item.put("basic_info", basic);

            Exam ex = examMapper.selectById(r.getExamId());
            int pageCount = (ex != null && ex.getType() != null ? ex.getType() : 0) + 1;
            List<String> picUrls = new ArrayList<>();
            for (int i = 0; i < pageCount; i++) {
                picUrls.add(minioService.presignedGetUrl(r.getId() + "_" + i + "_thumbnail", Duration.ofDays(1)));
            }
            item.put("pic_urls", picUrls);
            out.add(item);
        }
        return out;
    }

    /** 接口文档中学号多为 JSON 数字；能解析为 long 则输出数字，否则原样字符串 */
    private static Object studentIdForApi(String studentIdStr) {
        if (studentIdStr == null || studentIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(studentIdStr.trim());
        } catch (NumberFormatException e) {
            return studentIdStr;
        }
    }
}

