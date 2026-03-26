package com.geollm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, com.geollm.utils.ErrMsgUtil.get(200), data);
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg == null || msg.isBlank() ? com.geollm.utils.ErrMsgUtil.get(code) : msg, null);
    }
}

