package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.DashboardSummary;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.DashboardMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.DashboardService;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
@Tag(name = "首页", description = "首页/数据总览相关接口")
public class ApiHomeController {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> summaryResult = dashboardService.summary(0);
        Object data = summaryResult == null ? null : summaryResult.get("data");

        Long totalPatients = null;
        Double weekFollowRate = null;
        Long weekFollowDone = null;
        Long highRiskCount = null;
        Long midRiskCount = null;
        Long lowRiskCount = null;
        Double highRiskRatio = null;

        if (data instanceof DashboardSummary) {
            DashboardSummary summary = (DashboardSummary) data;
            totalPatients = summary.getPatientTotal();
            if (summary.getFollowUpWeek() != null) {
                weekFollowRate = summary.getFollowUpWeek().getCompletionRate();
                weekFollowDone = summary.getFollowUpWeek().getCompleted();
            }
            if (summary.getRiskLevel() != null) {
                highRiskCount = summary.getRiskLevel().getHigh();
                midRiskCount = summary.getRiskLevel().getMid();
                lowRiskCount = summary.getRiskLevel().getLow();
                Long total = summary.getRiskLevel().getTotal();
                if (total == null || total == 0) {
                    highRiskRatio = 0.0;
                } else {
                    highRiskRatio = (highRiskCount == null ? 0.0 : (highRiskCount * 1.0 / total));
                }
            }
        }

        String totalPatientsChange = buildPatientWeekChangeText();

        long processingAlerts = safeLong(dashboardMapper.countProcessingHealthAlerts())
                + safeLong(dashboardMapper.countProcessingDeviceAlerts());
        long redAlerts = safeLong(dashboardMapper.countRedProcessingHealthAlerts())
                + safeLong(dashboardMapper.countRedProcessingDeviceAlerts());

        Map<String, Object> dto = new HashMap<>();
        dto.put("totalPatients", totalPatients == null ? 0L : totalPatients);
        dto.put("totalPatientsChange", totalPatientsChange);
        dto.put("weekFollowRate", weekFollowRate == null ? 0.0 : weekFollowRate);
        dto.put("weekFollowDone", weekFollowDone == null ? 0L : weekFollowDone);
        dto.put("processingAlerts", processingAlerts);
        dto.put("redAlerts", redAlerts);
        dto.put("highRiskRatio", highRiskRatio == null ? 0.0 : highRiskRatio);
        dto.put("highRiskCount", highRiskCount == null ? 0L : highRiskCount);
        dto.put("midRiskCount", midRiskCount == null ? 0L : midRiskCount);
        dto.put("lowRiskCount", lowRiskCount == null ? 0L : lowRiskCount);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", dto);
        return result;
    }

    @GetMapping("/tables")
    public Map<String, Object> tables(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }

        Map<String, Object> data = new HashMap<>();

        List<Map<String, Object>> alerts = dashboardMapper.listRedProcessingAlerts(20);
        if (alerts != null) {
            for (Map<String, Object> row : alerts) {
                if (row == null) {
                    continue;
                }
                Long patientId = toLong(row.get("patientId"));
                String doctorName = patientId == null ? "" : safeStr(userMapper.findPatientResponsibleDoctorName(patientId));
                row.put("doctorName", doctorName);
            }
        }
        data.put("alerts", alerts);
        
        // 查询近期风险等级变为高危的患者
        List<Map<String, Object>> syndromes = userMapper.selectRecentHighRiskPatients(20);
        data.put("syndromes", syndromes != null ? syndromes : new ArrayList<>());
        data.put("followups", new ArrayList<>());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private User requireOperator(HttpServletRequest request) {
        Long userId = AuthHeaderUtil.getUserId(request);
        if (userId == null) {
            return null;
        }
        User u = userMapper.findById(userId);
        if (u == null || u.getRole() == null) {
            return null;
        }
        String role = u.getRole().trim().toUpperCase();
        if ("DOCTOR".equals(role) || "FOLLOW_UP".equals(role) || "ADMIN".equals(role)) {
            return u;
        }
        return null;
    }

    private String buildPatientWeekChangeText() {
        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate nextWeekStart = weekStart.plusDays(7);

        LocalDate lastWeekStart = weekStart.minusDays(7);
        LocalDate lastWeekEnd = weekStart;

        LocalDateTime thisStartAt = weekStart.atStartOfDay();
        LocalDateTime thisEndAt = nextWeekStart.atStartOfDay();

        LocalDateTime lastStartAt = lastWeekStart.atStartOfDay();
        LocalDateTime lastEndAt = lastWeekEnd.atStartOfDay();

        long thisWeekNew = safeLong(userMapper.countPatientsCreatedInRange(thisStartAt, thisEndAt));
        long lastWeekNew = safeLong(userMapper.countPatientsCreatedInRange(lastStartAt, lastEndAt));

        long delta = thisWeekNew - lastWeekNew;
        if (delta > 0) {
            return "+" + delta + "人";
        }
        if (delta < 0) {
            return delta + "人";
        }
        return "0人";
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
