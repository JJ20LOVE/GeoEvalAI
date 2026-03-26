package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.student.BaseStudentDto;
import com.geollm.dto.student.StudentDto;
import com.geollm.entity.Student;
import com.geollm.service.StudentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/student")
public class StudentController {
    private final StudentService studentService;
    private final com.geollm.service.XueqingService xueqingService;

    public StudentController(StudentService studentService, com.geollm.service.XueqingService xueqingService) {
        this.studentService = studentService;
        this.xueqingService = xueqingService;
    }

    @GetMapping("/getAllStudent")
    public ApiResponse<List<Student>> getAllStudent() {
        long t0 = System.currentTimeMillis();
        var data = studentService.getAll();
        log.info("GET /student/getAllStudent size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/getStudentById")
    public ApiResponse<Student> getStudentById(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("GET /student/getStudentById id={}", id);
        Student s = studentService.getById(id);
        log.info("GET /student/getStudentById done found={} costMs={}", s != null, System.currentTimeMillis() - t0);
        if (s == null) return ApiResponse.error(500, "not found");
        return ApiResponse.ok(s);
    }

    @GetMapping("/getStudentByClass")
    public ApiResponse<List<Student>> getStudentByClass(@RequestParam("id") Integer classId) {
        long t0 = System.currentTimeMillis();
        var data = studentService.getByClass(classId);
        log.info("GET /student/getStudentByClass id={} size={} costMs={}", classId, data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @PostMapping("/addStudent")
    public ApiResponse<Void> addStudent(@Valid @RequestBody BaseStudentDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("POST /student/addStudent student_id={} class_id={}", dto.getStudent_id(), dto.getClass_id());
        int code = studentService.add(dto.getStudent_id(), dto.getStudent_name(), dto.getClass_id());
        log.info("POST /student/addStudent done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "add student failed");
        return ApiResponse.ok(null);
    }

    @PutMapping("/updateStudent")
    public ApiResponse<Void> updateStudent(@Valid @RequestBody StudentDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("PUT /student/updateStudent id={} student_id={} class_id={}", dto.getId(), dto.getStudent_id(), dto.getClass_id());
        Student s = new Student();
        s.setId(dto.getId());
        s.setStudentId(dto.getStudent_id());
        s.setStudentName(dto.getStudent_name());
        s.setClassId(dto.getClass_id());
        int code = studentService.update(s);
        log.info("PUT /student/updateStudent done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "update student failed");
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/deleteStudent")
    public ApiResponse<Void> deleteStudent(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("DELETE /student/deleteStudent id={}", id);
        int code = studentService.delete(id);
        log.info("DELETE /student/deleteStudent done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "delete student failed");
        return ApiResponse.ok(null);
    }

    // Go 里还有 getStudentInfo（学情+历史），会在 xueqing 迁移时一起补齐
    @GetMapping("/getStudentInfo")
    public ApiResponse<?> getStudentInfo(@RequestParam("id") Integer id) {
        long t0 = System.currentTimeMillis();
        log.info("GET /student/getStudentInfo id={}", id);
        var info = xueqingService.getStudentInfo(id);
        log.info("GET /student/getStudentInfo done ok={} costMs={}", info != null, System.currentTimeMillis() - t0);
        if (info == null) return ApiResponse.error(400, null);
        return ApiResponse.ok(info);
    }
}

