package com.geollm.service.serviceimpl.ocr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geollm.entity.ExamOcrRoi;
import com.geollm.mapper.ExamOcrRoiMapper;
import com.geollm.service.ocr.TemplateOcrService;
import com.geollm.utils.ocr.AliyunOcrClient;
import com.geollm.utils.ocr.ImagePreprocessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateOcrServiceImpl implements TemplateOcrService {
    private final ExamOcrRoiMapper roiMapper;
    private final AliyunOcrClient aliyun;
    private final int timeoutSeconds;

    public TemplateOcrServiceImpl(ExamOcrRoiMapper roiMapper,
                                   @Value("${ocr.aliyun.host}") String host,
                                   @Value("${ocr.aliyun.path}") String path,
                                   @Value("${ocr.aliyun.appCode}") String appCode,
                                   @Value("${ocr.timeoutSeconds}") int timeoutSeconds) {
        this.roiMapper = roiMapper;
        this.timeoutSeconds = timeoutSeconds;
        this.aliyun = new AliyunOcrClient(host, path, appCode, Duration.ofSeconds(timeoutSeconds));
    }

    @Override
    public Map<Integer, String> ocrByTemplate(Integer examId, List<byte[]> pageImagesBytes, int qnumber) {
        // ROI 切题 OCR：
        // 1) 从 exam_ocr_roi 取出每个 qid 对应的坐标（可能跨多个页面/多段）
        // 2) 按 qid 分组 + pageIndex 升序，保证拼接顺序稳定
        // 3) 对每个 ROI：crop -> handwriting 预处理 -> 调用 Aliyun OCR（失败重试）
        // 4) 把同一 qid 的多段结果拼成一个文本（按页顺序用换行分隔）
        List<ExamOcrRoi> rois = roiMapper.selectList(
                new LambdaQueryWrapper<ExamOcrRoi>()
                        .eq(ExamOcrRoi::getExamId, examId)
                        .orderByAsc(ExamOcrRoi::getQid)
                        .orderByAsc(ExamOcrRoi::getPageIndex)
        );
        if (rois == null || rois.isEmpty()) return null;

        Map<Integer, List<ExamOcrRoi>> byQid = new HashMap<>();
        for (var r : rois) {
            byQid.computeIfAbsent(r.getQid(), k -> new ArrayList<>()).add(r);
        }
        for (var e : byQid.entrySet()) {
            // 同一个 qid 的 ROI 可能分布在不同页面，这里按页面顺序保证输出可预期
            e.getValue().sort(Comparator.comparingInt(ExamOcrRoi::getPageIndex));
        }

        List<BufferedImage> pages = new ArrayList<>();
        try {
            for (byte[] b : pageImagesBytes) pages.add(ImagePreprocessor.decode(b));
        } catch (Exception ex) {
            return null;
        }

        Map<Integer, String> result = new HashMap<>();
        for (int qid = 1; qid <= qnumber; qid++) {
            List<ExamOcrRoi> list = byQid.getOrDefault(qid, List.of());
            StringBuilder sb = new StringBuilder();
            for (ExamOcrRoi roi : list) {
                int p = roi.getPageIndex();
                if (p < 0 || p >= pages.size()) continue;
                try {
                    // 关键步骤：按 ROI 坐标裁剪 -> 二值化/膨胀增强笔画 -> 编码成 PNG -> OCR
                    BufferedImage crop = ImagePreprocessor.cropByRatio(pages.get(p), roi.getX1(), roi.getY1(), roi.getX2(), roi.getY2());
                    BufferedImage pre = ImagePreprocessor.preprocessHandwriting(crop);
                    byte[] png = ImagePreprocessor.encodePng(pre);
                    String text = callWithRetry(png);
                    if (text != null && !text.isBlank()) {
                        if (!sb.isEmpty()) sb.append("\n");
                        sb.append(text.trim());
                    }
                } catch (Exception ignored) {
                }
            }
            result.put(qid, sb.toString());
        }
        return result;
    }

    private String callWithRetry(byte[] imageBytes) throws Exception {
        // Aliyun OCR 调用失败可能是网络/限流问题，这里做简单指数递增等待重试
        Exception last = null;
        for (int i = 0; i < 3; i++) {
            try {
                return aliyun.ocr(imageBytes, timeoutSeconds);
            } catch (Exception e) {
                last = e;
                Thread.sleep(200L * (i + 1));
            }
        }
        if (last != null) throw last;
        return "";
    }
}

