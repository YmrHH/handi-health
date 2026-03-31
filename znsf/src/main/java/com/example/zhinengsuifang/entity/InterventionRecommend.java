package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterventionRecommend {
    private Long id;
    private String sourceType;
    private Long sourceId;
    private Long patientId;
    private String patientName;
    private String riskLevel;
    private String triggerReason;
    private LocalDateTime triggerTime;
    private String bodyType;
    private String pattern;
    private String disease;
    private String doctor;
    private String status;
    private String recommendation;
    private LocalDateTime sentTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
