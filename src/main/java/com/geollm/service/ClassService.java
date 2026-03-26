package com.geollm.service;

import com.geollm.entity.Class;

import java.util.List;

public interface ClassService {
    List<Class> getAll();

    int add(String className);

    int update(Integer classId, String className);

    int delete(Integer classId);
}

