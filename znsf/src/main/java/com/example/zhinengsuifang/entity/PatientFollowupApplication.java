package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientFollowupApplication {
    private Long id;
    private Long patientId;
    private String applyReason;
    private LocalDateTime preferredTime;
    private String contactPhone;
    private String status;
    private Long handledByUserId;
    private LocalDateTime handledAt;
    private String handleNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
