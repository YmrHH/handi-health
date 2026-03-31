package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "LoginByPhoneRequest", description = "Web 端医生手机号登录请求")
public class LoginByPhoneRequest {
    @Schema(description = "手机号（医生登录使用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    private String phone;

    @Schema(description = "密码（明文）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String password;
}
