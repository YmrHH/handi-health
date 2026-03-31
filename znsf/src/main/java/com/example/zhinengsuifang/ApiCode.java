package com.example.zhinengsuifang;

public enum ApiCode {
    SUCCESS(0, "成功"),

    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40101, "未授权"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "资源冲突"),
    VALIDATION_ERROR(42201, "参数校验失败"),
    INTERNAL_ERROR(50000, "系统异常");

    private final int code;
    private final String message;

    ApiCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
