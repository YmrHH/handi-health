package com.example.zhinengsuifang.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "User", description = "用户")
public class User {
    @Schema(description = "用户ID")
    private Long id;

    /** 登录账号（全系统唯一） */
    @Schema(description = "登录账号（全系统唯一）")
    private String username;

    /** 加密后密码（BCrypt） */
    @Schema(description = "加密后密码（BCrypt）")
    private String password;

    /** 真实姓名 */
    @Schema(description = "真实姓名")
    private String name;

    /** 住址 */
    @Schema(description = "住址")
    private String address;

    /** 年齡 */
    @Schema(description = "年龄")
    private Integer age;

    /** 性別 */
    @Schema(description = "性别")
    private String sex;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "风险等级")
    private String riskLevel;

    /**
     * 角色：DOCTOR（医生）、PATIENT（患者）、FOLLOW_UP（随访员）
     */
    @Schema(description = "角色：DOCTOR/PATIENT/FOLLOW_UP")
    private String role;

    /** 状态：1=启用，0=停用 */
    @Schema(description = "状态：1=启用，0=停用")
    private Integer status;
}
