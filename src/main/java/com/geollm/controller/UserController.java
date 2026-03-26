package com.geollm.controller;

import com.geollm.dto.ApiResponse;
import com.geollm.dto.user.ChangePassParam;
import com.geollm.dto.user.LoginParam;
import com.geollm.dto.user.SignUpParam;
import com.geollm.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody SignUpParam p) {
        long t0 = System.currentTimeMillis();
        log.info("POST /user/signup name={} email={}", p.getName(), p.getEmail());
        int code = userService.signUp(p);
        log.info("POST /user/signup done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "signup failed");
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginParam p) {
        long t0 = System.currentTimeMillis();
        log.info("POST /user/login username={}", p.getUsername());
        String token = userService.login(p);
        log.info("POST /user/login done ok={} costMs={}", token != null, System.currentTimeMillis() - t0);
        if (token == null) return ApiResponse.error(311, "login failed");
        return ApiResponse.ok(Map.of("token", token));
    }

    @PutMapping("/changePass")
    public ApiResponse<Void> changePass(@Valid @RequestBody ChangePassParam p) {
        long t0 = System.currentTimeMillis();
        log.info("PUT /user/changePass username={}", p.getUsername());
        int code = userService.changePassword(p);
        log.info("PUT /user/changePass done code={} costMs={}", code, System.currentTimeMillis() - t0);
        if (code != 200) return ApiResponse.error(code, "change password failed");
        return ApiResponse.ok(null);
    }
}

