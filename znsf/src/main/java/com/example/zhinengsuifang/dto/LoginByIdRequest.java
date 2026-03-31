package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "LoginByIdRequest", description = "小程序端基于用户ID登录请求")
public class LoginByIdRequest {
    @Schema(description = "用户ID（医生注册成功后返回的 userId）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long userId;

    @Schema(description = "密码（明文）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String password;
}
