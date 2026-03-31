package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AddUserRequest", description = "新增用户请求（仅允许创建患者/随访员）")
public class AddUserRequest {

    @Schema(description = "登录账号（全系统唯一）")
    private String username;

    @Schema(description = "密码（如该接口用于直接写库，需要传入已加密或明文，取决于后端逻辑）")
    private String password;

    @Schema(description = "真实姓名")
    private String name;

    @Schema(description = "住址")
    private String address;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "角色，仅允许 PATIENT/FOLLOW_UP", allowableValues = {"PATIENT", "FOLLOW_UP"})
    private String role;

    @Schema(description = "状态：1=启用，0=停用")
    private Integer status;
}
