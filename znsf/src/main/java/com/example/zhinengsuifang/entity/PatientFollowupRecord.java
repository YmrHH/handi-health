package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientFollowupRecord {
    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long staffId;
    private LocalDateTime followupTime;
    private String followupType;
    private String resultStatus;
    private String riskLevel;
    private String contentSummary;
    private String symptomChange;
    private Double sbp;
    private Double dbp;
    private Double heartRate;
    private Double weightKg;
    private LocalDateTime vitalMeasureTime;
    private String medAdherence;
    private String medPlanSummary;
    private String adverseReaction;
    private String tcmFace;
    private String tcmTongueBody;
    private String tcmTongueCoating;
    private String tcmTongueImageUrl;
    private String tcmPulseRate;
    private String tcmPulseTypes;
    private String tcmConclusion;
    private String tcmRemark;
    private String labSummary;
    private String nextFollowupType;
    private LocalDateTime nextFollowupDate;
    private String nextFollowupRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
