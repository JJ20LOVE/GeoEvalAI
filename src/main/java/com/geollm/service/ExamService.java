package com.geollm.service;

import com.geollm.dto.exam.YiTuoDto;
import com.geollm.entity.Exam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExamService {
    List<Exam> getAll();

    int updateTitle(Integer examId, String title);

    int delete(Integer examId);

    int deUploader(Integer examId);

    Integer addExam(String title, Integer creater, Integer qnumber, Integer type,
                     MultipartFile questionDocx, MultipartFile answerDocx) throws Exception;

    YiTuoDto getExamDetail(Integer examId) throws Exception;

    int yituo(YiTuoDto dto) throws Exception;
}

