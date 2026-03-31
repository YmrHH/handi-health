package com.example.zhinengsuifang.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardSyndromeAbnormalPatient {
    private Long patientId;
    private String name;
    private String phone;
    private String riskLevel;

    private String syndromeCode;
    private String syndromeName;
    private Double score;
    private LocalDateTime assessedAt;
}
