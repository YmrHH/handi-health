package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "RegisterRequest", description = "注册请求")
public class RegisterRequest {
    @Schema(description = "医生姓名（可重复）", example = "张三")
    private String name;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "确认密码（需与 password 一致）", example = "123456")
    private String confirmPassword;

    @Schema(description = "手机号（医生 Web 端登录使用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    private String phone;
}
