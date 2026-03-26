package com.geollm.service.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geollm.dto.recommendation.RecommendationFeedbackDto;
import com.geollm.dto.recommendation.SimilarQuestionDto;
import com.geollm.entity.RecommendationFeedback;
import com.geollm.entity.Wrongbook;
import com.geollm.mapper.RecommendationFeedbackMapper;
import com.geollm.service.RecommendationService;
import com.geollm.service.WrongbookService;
import com.geollm.service.ai.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final WrongbookService wrongbookService;
    private final RecommendationFeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeepSeekClient deepSeekClient;

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public RecommendationServiceImpl(
            WrongbookService wrongbookService,
            RecommendationFeedbackMapper feedbackMapper,
            DeepSeekClient deepSeekClient,
            @Value("${ai.deepseek.apiKey}") String apiKey,
            @Value("${ai.model}") String model,
            @Value("${ai.maxTokens}") int maxTokens,
            @Value("${ai.temperature}") double temperature
    ) {
        this.wrongbookService = wrongbookService;
        this.feedbackMapper = feedbackMapper;
        this.deepSeekClient = deepSeekClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public List<SimilarQuestionDto> getSimilarQuestions(int wrongId, int limit) {
        long t0 = System.currentTimeMillis();
        log.info("Recommendation.getSimilarQuestions wrongId={} limit={}", wrongId, limit);
        // 流程：
        // 1) 从错题本 wrongId 获取“原题 + 知识点”
        // 2) 调用 DeepSeek：要求返回“只包含 JSON 数组”的结果
        // 3) 清洗 code block 标记并反序列化成 DTO；失败则走 mock 兜底
        Wrongbook w = wrongbookService.getById(wrongId);
        if (w == null) {
            log.info("Recommendation.getSimilarQuestions notFound wrongId={}", wrongId);
            return null;
        }

        if (apiKey == null || apiKey.isBlank()) {
            var list = mock(w.getKnowledgePoint(), limit);
            log.info("Recommendation.getSimilarQuestions done viaMock size={} costMs={}", list.size(), System.currentTimeMillis() - t0);
            return list;
        }

        String prompt = """
                你是一个地理学科教育专家，请根据以下地理题目推荐%d道同类题目：

                原题目：%s
                知识点：%s

                要求：
                1. 题目类型和难度与原题相似，都是地理主观题
                2. 围绕相同的知识点或相关地理概念
                3. 返回格式必须是纯JSON数组，不要有任何其他文字
                4. JSON数组包含%d个对象，每个对象包含以下字段：
                   - question_id: 从1001开始递增的数字
                   - question_text: 题目内容
                   - knowledge_point: 知识点
                   - difficulty: 难度级别，只能是"简单"、"中等"、"困难"之一
                """.formatted(limit, w.getQuestionText(), w.getKnowledgePoint(), limit);

        String content = deepSeekClient.chatCompletion(model, prompt, maxTokens, temperature);
        if (content == null) {
            var list = mock(w.getKnowledgePoint(), limit);
            log.warn("Recommendation.getSimilarQuestions llmNull -> mock size={} costMs={}", list.size(), System.currentTimeMillis() - t0);
            return list;
        }

        String cleaned = cleanJson(content);
        try {
            List<SimilarQuestionDto> list = objectMapper.readValue(cleaned, new TypeReference<>() {});
            // 深度学习模型可能会返回空数组/奇怪结构，这里统一按空结果兜底
            if (list == null || list.isEmpty()) {
                var out = mock(w.getKnowledgePoint(), limit);
                log.warn("Recommendation.getSimilarQuestions llmEmpty -> mock size={} costMs={}", out.size(), System.currentTimeMillis() - t0);
                return out;
            }
            log.info("Recommendation.getSimilarQuestions done viaLLM size={} costMs={}", list.size(), System.currentTimeMillis() - t0);
            return list;
        } catch (Exception e) {
            var out = mock(w.getKnowledgePoint(), limit);
            log.warn("Recommendation.getSimilarQuestions llmParseFail -> mock size={} costMs={} err={}",
                    out.size(), System.currentTimeMillis() - t0, e.toString());
            return out;
        }
    }

    @Override
    public int addFeedback(RecommendationFeedbackDto dto) {
        log.info("Recommendation.addFeedback studentId={} wrongId={} questionId={}",
                dto.getStudent_id(), dto.getWrong_id(), dto.getQuestion_id());
        RecommendationFeedback e = new RecommendationFeedback();
        e.setStudentId(dto.getStudent_id());
        e.setWrongId(dto.getWrong_id());
        e.setQuestionId(dto.getQuestion_id());
        e.setFeedback(dto.getFeedback());
        feedbackMapper.insert(e);
        log.info("Recommendation.addFeedback inserted id={}", e.getId());
        return 200;
    }

    private List<SimilarQuestionDto> mock(String kp, int limit) {
        List<SimilarQuestionDto> all = new ArrayList<>();
        all.add(sim(1001, "分析导致珠江三角洲人口密集的自然因素。", kp, "中等"));
        all.add(sim(1002, "说明四川盆地人口稠密的自然条件。", kp, "中等"));
        all.add(sim(1003, "比较长江三角洲和珠江三角洲的人口分布特征。", kp, "困难"));
        all.add(sim(1004, "分析黄河流域人口分布的主要影响因素。", kp, "中等"));
        all.add(sim(1005, "说明地形对华北平原人口分布的影响。", kp, "简单"));
        if (limit > 0 && limit < all.size()) return all.subList(0, limit);
        return all;
    }

    private SimilarQuestionDto sim(int id, String text, String kp, String diff) {
        SimilarQuestionDto s = new SimilarQuestionDto();
        s.setQuestion_id(id);
        s.setQuestion_text(text);
        s.setKnowledge_point(kp);
        s.setDifficulty(diff);
        return s;
    }

    private String cleanJson(String content) {
        // 模型有时会用 ```json 包裹输出，这里去掉围栏，保证 JSON 可解析
        String c = content.trim();
        if (c.startsWith("```json")) c = c.substring(7).trim();
        if (c.startsWith("```")) c = c.substring(3).trim();
        if (c.endsWith("```")) c = c.substring(0, c.length() - 3).trim();
        return c;
    }
}

