package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
public class DashboardSummary {
    private Long patientTotal;

    private FollowUpWeekStats followUpWeek;

    private AlertStats alerts;

    private RiskLevelStats riskLevel;

    @Data
    public static class FollowUpWeekStats {
        private Long total;
        private Long completed;
        private Double completionRate;
    }

    @Data
    public static class AlertStats {
        private Long dataAbnormal;
        private Long hardwareAbnormal;
        private Long total;
    }

    @Data
    public static class RiskLevelStats {
        private Long low;
        private Long mid;
        private Long high;
        private Long total;
    }
}
