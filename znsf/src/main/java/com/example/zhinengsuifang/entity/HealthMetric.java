package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 健康指标实体。
 */
public class HealthMetric {
    private Long id;

    private Long patientId;

    private String metricType;

    private Double value1;

    private Double value2;

    private LocalDateTime measuredAt;

    private Long createdByUserId;

    private LocalDateTime createdAt;
}

