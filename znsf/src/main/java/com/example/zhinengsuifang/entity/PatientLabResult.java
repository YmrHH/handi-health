package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientLabResult {
    private Long id;
    private Long patientId;
    private String labType;
    private String valueText;
    private Double valueNum;
    private String unit;
    private String status;
    private LocalDateTime measuredAt;
    private String hospital;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
