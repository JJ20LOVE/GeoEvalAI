package com.geollm.service.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geollm.dto.recommendation.RecommendationFeedbackDto;
import com.geollm.dto.recommendation.SimilarQuestionDto;
import com.geollm.entity.RecommendationBatch;
import com.geollm.entity.RecommendationFeedback;
import com.geollm.entity.RecommendationItem;
import com.geollm.entity.Wrongbook;
import com.geollm.mapper.RecommendationBatchMapper;
import com.geollm.mapper.RecommendationFeedbackMapper;
import com.geollm.mapper.RecommendationItemMapper;
import com.geollm.service.RecommendationService;
import com.geollm.service.WrongbookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final WrongbookService wrongbookService;
    private final RecommendationBatchMapper batchMapper;
    private final RecommendationItemMapper itemMapper;
    private final RecommendationFeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RecommendationServiceImpl(
            WrongbookService wrongbookService,
            RecommendationBatchMapper batchMapper,
            RecommendationItemMapper itemMapper,
            RecommendationFeedbackMapper feedbackMapper,
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) VectorStore vectorStore
    ) {
        this.wrongbookService = wrongbookService;
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.feedbackMapper = feedbackMapper;
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @Override
    public List<SimilarQuestionDto> getSimilarQuestions(int wrongId, int limit) {
        long t0 = System.currentTimeMillis();
        log.info("Recommendation.getSimilarQuestions wrongId={} limit={}", wrongId, limit);
        // 流程：
        // 1) 从错题本 wrongId 获取“原题 + 知识点”
        // 2) 向量库检索 TopK 候选题
        // 3) 调用 LLM：要求返回“只包含 JSON 数组”的结果
        // 4) 清洗 code block 标记并反序列化成 DTO；失败则走 mock 兜底
        Wrongbook w = wrongbookService.getById(wrongId);
        if (w == null) {
            log.info("Recommendation.getSimilarQuestions notFound wrongId={}", wrongId);
            return null;
        }

        List<Document> candidates;
        if (vectorStore == null) {
            candidates = List.of();
            log.warn("Recommendation.search skipped because vectorStore is null");
        } else {
            try {
                int topK = Math.max(10, limit * 5);
                String query = (w.getKnowledgePoint() == null ? "" : w.getKnowledgePoint() + "\n") + w.getQuestionText();
                candidates = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(topK)
                                .filterExpression("kb == 'default'")
                                .build()
                );
            } catch (Exception e) {
                candidates = List.of();
                log.warn("Recommendation.search failed -> fallback err={}", e.toString());
            }
        }

        StringBuilder refs = new StringBuilder();
        int idx = 0;
        for (Document d : candidates) {
            if (idx >= 10) break;
            refs.append("[").append(idx + 1).append("]\n");
            refs.append(d.getText()).append("\n\n");
            idx++;
        }

        String prompt = """
                你是一个地理学科教研员。请基于“原题”与“参考题库候选题”（候选题可能噪声）生成%d道同类题。

                原题目：
                %s

                知识点：
                %s

                参考题库候选题（用于保持同类性，不要直接照抄）：
                %s

                要求：
                1) 题型保持为地理主观题，设问方式与原题相似
                2) 知识点必须与原题一致或高度相关
                3) 难度字段只能是：简单/中等/困难
                4) 返回必须是“纯 JSON 数组”，不要任何额外文字
                5) JSON数组包含%d个对象，每个对象包含字段：
                   question_id（从1001递增）、question_text、knowledge_point、difficulty
                """.formatted(limit,
                w.getQuestionText(),
                w.getKnowledgePoint() == null ? "" : w.getKnowledgePoint(),
                refs.isEmpty() ? "(无候选题)" : refs.toString(),
                limit);

        String content;
        try {
            content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            content = null;
            log.warn("Recommendation.llm call failed err={}", e.toString());
        }

        if (content == null) {
            var list = mock(w.getKnowledgePoint(), limit);
            persistRecommendationResults(w, limit, list);
            log.warn("Recommendation.getSimilarQuestions llmNull -> mock size={} costMs={}", list.size(), System.currentTimeMillis() - t0);
            return list;
        }

        String cleaned = cleanJson(content);
        try {
            List<SimilarQuestionDto> list = objectMapper.readValue(cleaned, new TypeReference<>() {});
            // 深度学习模型可能会返回空数组/奇怪结构，这里统一按空结果兜底
            if (list == null || list.isEmpty()) {
                var out = mock(w.getKnowledgePoint(), limit);
                persistRecommendationResults(w, limit, out);
                log.warn("Recommendation.getSimilarQuestions llmEmpty -> mock size={} costMs={}", out.size(), System.currentTimeMillis() - t0);
                return out;
            }
            persistRecommendationResults(w, limit, list);
            log.info("Recommendation.getSimilarQuestions done viaLLM size={} costMs={}", list.size(), System.currentTimeMillis() - t0);
            return list;
        } catch (Exception e) {
            var out = mock(w.getKnowledgePoint(), limit);
            persistRecommendationResults(w, limit, out);
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

    private void persistRecommendationResults(Wrongbook w, int limit, List<SimilarQuestionDto> list) {
        if (w == null || list == null || list.isEmpty()) return;
        try {
            RecommendationBatch batch = new RecommendationBatch();
            batch.setStudentId(w.getStudentId());
            batch.setWrongId(w.getWrongId());
            batch.setSourceQuestionText(w.getQuestionText());
            batch.setSourceKnowledgePoint(w.getKnowledgePoint());
            batch.setModelName("deepseek-chat");
            batch.setRequestLimit(limit);
            batchMapper.insert(batch);

            Long batchId = batch.getBatchId();
            if (batchId == null) {
                log.warn("Recommendation.persist batchId is null, skip item persistence");
                return;
            }

            int itemNo = 1;
            for (SimilarQuestionDto dto : list) {
                if (dto == null) continue;
                RecommendationItem item = new RecommendationItem();
                item.setBatchId(batchId);
                item.setItemNo(itemNo++);
                item.setQuestionText(dto.getQuestion_text());
                item.setKnowledgePoint(dto.getKnowledge_point());
                item.setDifficulty(dto.getDifficulty());
                itemMapper.insert(item);

                dto.setBatch_id(batchId);
                dto.setItem_id(item.getItemId());
            }
            log.info("Recommendation.persist done batchId={} itemSize={}", batchId, list.size());
        } catch (Exception e) {
            // 落表失败不影响主流程返回，避免推荐接口因存储异常不可用
            log.warn("Recommendation.persist failed err={}", e.toString());
        }
    }
}

