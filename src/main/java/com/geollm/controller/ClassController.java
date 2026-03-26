package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.classroom.BaseClassDto;
import com.geollm.dto.classroom.ClassDto;
import com.geollm.entity.Class;
import com.geollm.service.ClassService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/class")
public class ClassController {
    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping("/getAllClass")
    public ApiResponse<List<Class>> getAllClass() {
        long t0 = System.currentTimeMillis();
        var data = classService.getAll();
        log.info("GET /class/getAllClass size={} costMs={}", data == null ? 0 : data.size(), System.currentTimeMillis() - t0);
        return ApiResponse.ok(data);
    }

    @PostMapping("/addClass")
    public ApiResponse<Void> addClass(@Valid @RequestBody BaseClassDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("POST /class/addClass class_name={}", dto.getClass_name());
        int code = classService.add(dto.getClass_name());
        log.info("POST /class/addClass done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "add class failed");
        return ApiResponse.ok(null);
    }

    @PutMapping("/updateClass")
    public ApiResponse<Void> updateClass(@Valid @RequestBody ClassDto dto) {
        long t0 = System.currentTimeMillis();
        log.info("PUT /class/updateClass class_id={} class_name={}", dto.getClass_id(), dto.getClass_name());
        int code = classService.update(dto.getClass_id(), dto.getClass_name());
        log.info("PUT /class/updateClass done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "update class failed");
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/deleteClass")
    public ApiResponse<Void> deleteClass(@RequestParam("class_id") Integer classId) {
        long t0 = System.currentTimeMillis();
        log.info("DELETE /class/deleteClass class_id={}", classId);
        int code = classService.delete(classId);
        log.info("DELETE /class/deleteClass done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "delete class failed");
        return ApiResponse.ok(null);
    }
}

