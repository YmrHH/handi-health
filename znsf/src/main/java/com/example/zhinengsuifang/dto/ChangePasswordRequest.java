package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ChangePasswordRequest", description = "小程序端修改密码请求")
public class ChangePasswordRequest {
    @Schema(description = "用户ID（医生注册成功后返回的 userId）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long userId;

    @Schema(description = "旧密码（明文）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String oldPassword;

    @Schema(description = "新密码（明文）", requiredMode = Schema.RequiredMode.REQUIRED, example = "abc12345")
    private String newPassword;

    @Schema(description = "确认新密码（需与 newPassword 一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "abc12345")
    private String confirmPassword;
}
