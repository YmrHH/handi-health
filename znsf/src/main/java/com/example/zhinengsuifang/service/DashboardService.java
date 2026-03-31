package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.dto.DashboardSummary;
import com.example.zhinengsuifang.dto.DashboardSyndromeAbnormalPatient;
import com.example.zhinengsuifang.dto.DashboardSyndromeAlertTrends;
import com.example.zhinengsuifang.dto.PatientBrief;
import com.example.zhinengsuifang.dto.RiskLevelTrendPoint;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> summary(Integer weekOffset);

    Map<String, Object> riskLevelTrend(Integer days);

    Map<String, Object> syndromeAndAlertTrends(Integer days);

    Map<String, Object> highRiskPending(Integer limit);

    Map<String, Object> syndromeAbnormalPatients(Integer days, Integer limit);
}
