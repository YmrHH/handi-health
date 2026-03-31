package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskLevelHistory {
    private Long id;

    private Long patientId;

    private String riskLevel;

    private LocalDateTime assessedAt;

    private String source;

    private LocalDateTime createdAt;
}
