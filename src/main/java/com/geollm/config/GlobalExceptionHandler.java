package com.geollm.config;

import com.geollm.dto.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(Exception e) {
        return ApiResponse.error(300, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleBadJson(HttpMessageNotReadableException e) {
        return ApiResponse.error(201, null);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneric(Exception e) {
        return ApiResponse.error(400, e.getMessage());
    }
}

