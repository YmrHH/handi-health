package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DoctorAdvice {
    private Long id;

    private LocalDateTime adviceDate;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String description;

    private String patientsJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
