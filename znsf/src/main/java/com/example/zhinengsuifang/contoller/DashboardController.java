package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "看板", description = "看板统计相关接口")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) Integer weekOffset) {
        return dashboardService.summary(weekOffset);
    }

    @GetMapping("/riskLevel/trend")
    public Map<String, Object> riskLevelTrend(@RequestParam(required = false) Integer days) {
        return dashboardService.riskLevelTrend(days);
    }

    @GetMapping("/syndromeAlert/trend")
    public Map<String, Object> syndromeAlertTrend(@RequestParam(required = false) Integer days) {
        return dashboardService.syndromeAndAlertTrends(days);
    }

    @GetMapping("/highRisk/pending")
    public Map<String, Object> highRiskPending(@RequestParam(required = false) Integer limit) {
        return dashboardService.highRiskPending(limit);
    }

    @GetMapping("/syndrome/abnormalPatients")
    public Map<String, Object> syndromeAbnormalPatients(@RequestParam(required = false) Integer days,
                                                        @RequestParam(required = false) Integer limit) {
        return dashboardService.syndromeAbnormalPatients(days, limit);
    }
}
