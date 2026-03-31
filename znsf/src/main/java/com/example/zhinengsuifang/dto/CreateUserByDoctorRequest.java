package com.example.zhinengsuifang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
/**
 * 医生创建新用户（患者/随访员）请求 DTO。
 */
@Schema(name = "CreateUserByDoctorRequest", description = "医生创建新用户（患者/随访员）请求体")
public class CreateUserByDoctorRequest {
    @Schema(description = "医生用户ID（推荐，用于鉴权，必须为 DOCTOR）", example = "123")
    private Long doctorId;

    @Schema(description = "医生登录账号（用于鉴权，必须为 DOCTOR）", example = "doctor001")
    private String doctorUsername;

    @Schema(description = "医生登录密码（明文，用于鉴权）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String doctorPassword;

    @Schema(description = "新用户登录账号（手机号体系下可为空）", example = "patient001")
    private String username;

    @Schema(description = "新用户姓名/显示名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "住址", example = "广东省深圳市")
    private String address;

    @Schema(description = "年龄", example = "45")
    private Integer age;

    @Schema(description = "性别", example = "男")
    private String sex;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    private String phone;

    @Schema(description = "身份证号（仅当 role=PATIENT 时建议填写）", example = "11010519491231002X")
    private String idCard;

    @Schema(description = "病种（仅当 role=PATIENT 时可选）", example = "高血压")
    private String disease;

    @Schema(description = "证型（仅当 role=PATIENT 时可选）", example = "肝阳上亢")
    private String syndrome;

    @Schema(description = "体质（仅当 role=PATIENT 时可选）", example = "痰湿体质")
    private String constitution;

    @Schema(description = "家族遗传病史（仅当 role=PATIENT 时可选）", example = "父亲高血压")
    private String familyHistory;

    @Schema(description = "风险等级（仅当 role=PATIENT 时必填）", allowableValues = {"低", "中", "高", "上"}, example = "中")
    private String riskLevel;

    @Schema(description = "新用户角色（仅允许 PATIENT/FOLLOW_UP）", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"PATIENT", "FOLLOW_UP"}, example = "PATIENT")
    private String role;
}

