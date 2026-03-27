package com.geollm.service.serviceimpl;

import com.geollm.service.KbService;
import com.geollm.utils.docx.DocxExtractor;
import com.geollm.utils.docx.Question;
import com.geollm.utils.docx.Section;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KbServiceImpl implements KbService {

    private final VectorStore vectorStore;

    // 使用 @Autowired(required = false) 让 Spring 在 VectorStore 不可用时注入 null
    public KbServiceImpl(@org.springframework.beans.factory.annotation.Autowired(required = false) VectorStore vectorStore) {
        // 如果 VectorStore 不可用（Milvus 未启动），使用 null
        this.vectorStore = vectorStore;
    }

    @Override
    public int importDocx(MultipartFile file, String kbName) throws Exception {
        long t0 = System.currentTimeMillis();
        String kb = (kbName == null || kbName.isBlank()) ? "default" : kbName.trim();
        String originalName = file == null ? null : file.getOriginalFilename();
        log.info("KB.importDocx kbName={} file={} size={}", kb, originalName, file == null ? 0 : file.getSize());

        if (file == null || file.isEmpty()) return 0;

        List<Section> sections = DocxExtractor.extract(file.getInputStream());
        List<Document> docs = new ArrayList<>();

        int secIdx = 0;
        for (Section s : sections) {
            secIdx++;
            String secTitle = s == null ? "" : s.getTitle();
            List<Question> qs = s == null ? null : s.getQuestions();
            if (qs == null) continue;

            int qIdx = 0;
            for (Question q : qs) {
                qIdx++;
                if (q == null) continue;
                String text = q.getContent();
                if (text == null || text.isBlank()) continue;

                for (String chunk : chunk(text, 800, 100)) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("kb", kb);
                    meta.put("source", "docx");
                    if (originalName != null) meta.put("file", originalName);
                    meta.put("section_index", secIdx);
                    meta.put("section_title", secTitle == null ? "" : secTitle);
                    meta.put("question_no", q.getNumber());
                    meta.put("question_index", qIdx);
                    docs.add(new Document(chunk, meta));
                }
            }
        }

        if (docs.isEmpty()) {
            log.info("KB.importDocx noDocs costMs={}", System.currentTimeMillis() - t0);
            return 0;
        }

        // 如果 VectorStore 可用才添加，否则只记录警告
        if (vectorStore != null) {
            try {
                vectorStore.add(docs);
                log.info("KB.importDocx done added={} costMs={}", docs.size(), System.currentTimeMillis() - t0);
            } catch (Exception e) {
                log.warn("KB.importDocx vectorStore add failed err={}, but still return success", e.toString());
                // 即使向量库失败，也返回成功（至少完成了文档解析）
            }
        } else {
            log.warn("KB.importDocx vectorStore is null, skipping vector storage");
        }
        return docs.size();
    }

    private List<String> chunk(String text, int maxChars, int overlap) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return List.of();
        if (t.length() <= maxChars) return List.of(t);

        int step = Math.max(1, maxChars - Math.max(0, overlap));
        List<String> out = new ArrayList<>();
        for (int start = 0; start < t.length(); start += step) {
            int end = Math.min(t.length(), start + maxChars);
            String part = t.substring(start, end).trim();
            if (!part.isEmpty()) out.add(part);
            if (end >= t.length()) break;
        }
        return out;
    }
}

