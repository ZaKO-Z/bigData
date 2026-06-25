package com.example.cars;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 API 响应格式
 */
public class ApiResponse {

    private boolean success;
    private String message;
    private Object data;

    private ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse ok(Object data) {
        return new ApiResponse(true, "success", data);
    }

    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    public static ApiResponse fail(String message) {
        return new ApiResponse(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}