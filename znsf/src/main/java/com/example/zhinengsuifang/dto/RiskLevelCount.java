package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
/**
 * 风险等级统计行 DTO。
 */
public class RiskLevelCount {
    private String riskLevel;
    private Long count;
}

