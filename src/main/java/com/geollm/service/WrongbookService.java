package com.geollm.service;

import com.geollm.dto.wrongbook.WrongQuestionRequestDto;
import com.geollm.entity.Wrongbook;

import java.util.List;

public interface WrongbookService {
    int addOrUpdate(WrongQuestionRequestDto dto);

    List<Wrongbook> listByStudent(Integer studentId, String knowledgePoint);

    Wrongbook getById(Integer wrongId);

    int delete(Integer wrongId);
}

