package com.example.zhinengsuifang.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthAlertWithPatient {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;

    private String metricType;

    private Double prevValue1;
    private Double prevValue2;

    private Double currValue1;
    private Double currValue2;

    private Double deltaValue1;
    private Double deltaValue2;

    private String severity;
    private String status;

    private LocalDateTime createdAt;
}
