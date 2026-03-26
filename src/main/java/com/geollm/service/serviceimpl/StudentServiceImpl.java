package com.geollm.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geollm.entity.Class;
import com.geollm.entity.Student;
import com.geollm.mapper.ClassMapper;
import com.geollm.mapper.StudentMapper;
import com.geollm.service.StudentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {
    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;

    public StudentServiceImpl(StudentMapper studentMapper, ClassMapper classMapper) {
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
    }

    @Override
    public List<Student> getAll() {
        return studentMapper.selectList(null);
    }

    @Override
    public Student getById(Integer id) {
        return studentMapper.selectById(id);
    }

    @Override
    public List<Student> getByClass(Integer classId) {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId));
    }

    @Override
    public int add(String studentId, String studentName, Integer classId) {
        Class c = classMapper.selectById(classId);
        if (c == null) return 502;

        Long cnt = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentId, studentId));
        if (cnt != null && cnt > 0) return 501;

        Student s = new Student();
        s.setStudentId(studentId);
        s.setStudentName(studentName);
        s.setClassId(classId);
        studentMapper.insert(s);
        return 200;
    }

    @Override
    public int update(Student s) {
        Student existing = studentMapper.selectById(s.getId());
        if (existing == null) return 500;
        Class c = classMapper.selectById(s.getClassId());
        if (c == null) return 502;
        studentMapper.updateById(s);
        return 200;
    }

    @Override
    public int delete(Integer id) {
        Student existing = studentMapper.selectById(id);
        if (existing == null) return 500;
        studentMapper.deleteById(id);
        return 200;
    }
}

