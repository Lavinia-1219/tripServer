package com.tripcompanion.common;

public class ApiResponse<T> {

    private final boolean ok;
    private final T data;
    private final String code;
    private final String message;

    private ApiResponse(boolean ok, T data, String code, String message) {
        this.ok = ok;
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, code, message);
    }

    public boolean isOk() {
        return ok;
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
