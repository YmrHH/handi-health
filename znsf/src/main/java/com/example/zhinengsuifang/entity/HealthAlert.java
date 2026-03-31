package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 健康预警实体。
 */
public class HealthAlert {
    private Long id;

    private Long patientId;

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

