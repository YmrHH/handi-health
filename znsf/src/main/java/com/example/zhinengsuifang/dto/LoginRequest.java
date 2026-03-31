package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
/**
 * 登录请求 DTO。
 */
@Schema(name = "LoginRequest", description = "登录请求")
public class LoginRequest {
    @Schema(description = "用户名", example = "test001")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;
}
