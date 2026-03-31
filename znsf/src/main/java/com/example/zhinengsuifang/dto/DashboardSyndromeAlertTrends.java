package com.example.zhinengsuifang.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardSyndromeAlertTrends {
    private List<DashboardTrendPoint> syndromeStable;
    private List<DashboardTrendPoint> alerts;
}
