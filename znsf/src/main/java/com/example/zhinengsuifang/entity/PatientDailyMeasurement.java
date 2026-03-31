package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientDailyMeasurement {
    private Long id;
    private Long patientId;
    private LocalDateTime measuredAt;
    private Double sbp;
    private Double dbp;
    private Double heartRate;
    private Double weightKg;
    private Double temperatureC;
    private Double spo2;
    private Double glucose;
    private String glucoseType;
    private Double sleep;
    private String symptoms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
