package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TCMDiagnosis {
    private Long id;
    private Long patientId;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime diagnosisDate;
    private String mainComplaint;
    private String tongueDescription;
    private String tongueImageUrl;
    private String pulseDescription;
    private String tcmSummary;
    private String physicalExam;
    private String lifestyle;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}