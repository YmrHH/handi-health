package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
public class RiskLevelTrendPoint {
    private String day;
    private String riskLevel;
    private Long count;
}
