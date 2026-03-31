package com.example.zhinengsuifang.entity;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
/**
 * 复诊记录实体。
 */
public class RevisitRecord {
    private Long id;

    @NotNull(message = "patientId 不能为空")
    private Long patientId;

    @NotNull(message = "revisitAt 不能为空")
    private LocalDateTime revisitAt;

    @NotBlank(message = "hospital 不能为空")
    private String hospital;

    @NotBlank(message = "department 不能为空")
    private String department;

    private String doctorName;

    @NotBlank(message = "notes 不能为空")
    private String notes;

    private Long createdByDoctorId;

    private LocalDateTime createdAt;
}

