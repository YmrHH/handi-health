package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientLinkedAccount {
    private Long id;
    private Long patientId;
    private Long linkedUserId;
    private String relation;
    private String linkedUsername;
    private String linkedPhone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
