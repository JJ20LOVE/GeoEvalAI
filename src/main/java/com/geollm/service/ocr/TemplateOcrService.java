package com.geollm.service.ocr;

import java.util.List;
import java.util.Map;

public interface TemplateOcrService {
    Map<Integer, String> ocrByTemplate(Integer examId, List<byte[]> pageImagesBytes, int qnumber);
}

