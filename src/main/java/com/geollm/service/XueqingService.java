package com.geollm.service;

import com.geollm.dto.xueqing.*;

import java.util.List;
import java.util.Map;

public interface XueqingService {
    StudentInfoDto getStudentInfo(Integer id);

    List<QuestionScoresDto> getResultByQuestion(Integer examId);

    List<StudentScoresDto> getResultByStudent(Integer examId);

    List<Double> getQuestionPointRate(Integer examId);

    Map<String, List<StudentListDto>> getNameList(Integer examId);

    ExamDataDto getExamData(Integer examId);
}

