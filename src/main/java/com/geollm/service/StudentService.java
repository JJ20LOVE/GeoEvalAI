package com.geollm.service;

import com.geollm.entity.Student;

import java.util.List;

public interface StudentService {
    List<Student> getAll();

    Student getById(Integer id);

    List<Student> getByClass(Integer classId);

    int add(String studentId, String studentName, Integer classId);

    int update(Student s);

    int delete(Integer id);
}

