package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.dto.*;
import com.example.zhinengsuifang.mapper.DashboardMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.DashboardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> summary(Integer weekOffset) {
        Map<String, Object> result = new HashMap<>();

        int offset = weekOffset == null ? 0 : weekOffset;
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY).plusWeeks(offset);
        LocalDate weekEndDate = weekStartDate.plusDays(7);

        LocalDateTime startAt = weekStartDate.atStartOfDay();
        LocalDateTime endAt = weekEndDate.atStartOfDay();

        Long patientTotal = userMapper.countPatients();

        Long weekTotal = dashboardMapper.countFollowUpDueInRange(startAt, endAt);
        Long weekCompleted = dashboardMapper.countFollowUpCompletedInRange(startAt, endAt);

        Long dataAbnormal = dashboardMapper.countDataAlertsInRange(startAt, endAt);
        Long hardwareAbnormal = dashboardMapper.countDeviceAlertsInRange(startAt, endAt);

        DashboardSummary summary = new DashboardSummary();
        summary.setPatientTotal(patientTotal == null ? 0L : patientTotal);

        DashboardSummary.FollowUpWeekStats followUpWeek = new DashboardSummary.FollowUpWeekStats();
        followUpWeek.setTotal(weekTotal == null ? 0L : weekTotal);
        followUpWeek.setCompleted(weekCompleted == null ? 0L : weekCompleted);
        if (weekTotal == null || weekTotal == 0) {
            followUpWeek.setCompletionRate(0.0);
        } else {
            followUpWeek.setCompletionRate((weekCompleted == null ? 0.0 : (weekCompleted * 1.0 / weekTotal)));
        }
        summary.setFollowUpWeek(followUpWeek);

        DashboardSummary.AlertStats alerts = new DashboardSummary.AlertStats();
        long da = dataAbnormal == null ? 0L : dataAbnormal;
        long ha = hardwareAbnormal == null ? 0L : hardwareAbnormal;
        alerts.setDataAbnormal(da);
        alerts.setHardwareAbnormal(ha);
        alerts.setTotal(da + ha);
        summary.setAlerts(alerts);

        DashboardSummary.RiskLevelStats riskLevel = new DashboardSummary.RiskLevelStats();
        riskLevel.setLow(0L);
        riskLevel.setMid(0L);
        riskLevel.setHigh(0L);

        List<RiskLevelCount> list = dashboardMapper.countPatientsByEffectiveRiskLevel();
        if (list != null) {
            for (RiskLevelCount row : list) {
                if (row == null || row.getRiskLevel() == null) {
                    continue;
                }
                String level = row.getRiskLevel().trim().toUpperCase();
                long cnt = row.getCount() == null ? 0L : row.getCount();
                if ("LOW".equals(level)) {
                    riskLevel.setLow(cnt);
                } else if ("MID".equals(level) || "MEDIUM".equals(level)) {
                    riskLevel.setMid(cnt);
                } else if ("HIGH".equals(level)) {
                    riskLevel.setHigh(cnt);
                }
            }
        }
        riskLevel.setTotal((riskLevel.getLow() == null ? 0L : riskLevel.getLow())
                + (riskLevel.getMid() == null ? 0L : riskLevel.getMid())
                + (riskLevel.getHigh() == null ? 0L : riskLevel.getHigh()));
        summary.setRiskLevel(riskLevel);

        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", summary);
        return result;
    }

    @Override
    public Map<String, Object> riskLevelTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();

        int d = days == null ? 30 : Math.max(1, Math.min(days, 365));
        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(d);

        List<RiskLevelTrendPoint> list = dashboardMapper.riskLevelTrend(startAt, endAt);

        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @Override
    public Map<String, Object> syndromeAndAlertTrends(Integer days) {
        Map<String, Object> result = new HashMap<>();

        int d = days == null ? 30 : Math.max(1, Math.min(days, 365));
        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(d);

        List<DashboardTrendPoint> stable = dashboardMapper.syndromeStableTrend(startAt, endAt);
        List<DashboardTrendPoint> alerts = dashboardMapper.dataAlertTrend(startAt, endAt);

        DashboardSyndromeAlertTrends dto = new DashboardSyndromeAlertTrends();
        dto.setSyndromeStable(stable);
        dto.setAlerts(alerts);

        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", dto);
        return result;
    }

    @Override
    public Map<String, Object> highRiskPending(Integer limit) {
        Map<String, Object> result = new HashMap<>();

        int l = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        List<PatientBrief> list = dashboardMapper.highRiskPendingList(l);

        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @Override
    public Map<String, Object> syndromeAbnormalPatients(Integer days, Integer limit) {
        Map<String, Object> result = new HashMap<>();

        int d = days == null ? 30 : Math.max(1, Math.min(days, 365));
        int l = limit == null ? 50 : Math.max(1, Math.min(limit, 200));

        LocalDateTime startAt = LocalDateTime.now().minusDays(d);
        List<DashboardSyndromeAbnormalPatient> list = dashboardMapper.syndromeAbnormalPatients(startAt, l);

        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }
}
