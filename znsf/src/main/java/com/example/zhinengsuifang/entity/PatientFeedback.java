package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientFeedback {
    private Long id;
    private Long patientId;
    private String feedbackType;
    private String content;
    private String contact;
    private String status;
    private String reply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
