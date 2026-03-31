package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
public class PatientBasicInfo {
    private Long id;
    private Long patientId;
    private Double heightCm;
    private Double baselineWeightKg;
    private LocalDate birthday;
    private String idCard;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String insuranceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
