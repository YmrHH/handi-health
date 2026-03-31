package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.DashboardSummary;
import com.example.zhinengsuifang.dto.DashboardTrendPoint;
import com.example.zhinengsuifang.dto.RiskLevelCount;
import com.example.zhinengsuifang.dto.RiskLevelTrendPoint;
import com.example.zhinengsuifang.mapper.DashboardMapper;
import com.example.zhinengsuifang.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/report")
@Tag(name = "报表", description = "报表/趋势相关接口")
public class ApiReportController {

    @Resource
    private DashboardMapper dashboardMapper;

    @Resource
    private DashboardService dashboardService;

    private static final class ExportFile {
        private final byte[] bytes;
        private final String fileName;
        private final MediaType mediaType;

        private ExportFile(byte[] bytes, String fileName, MediaType mediaType) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.mediaType = mediaType;
        }
    }

    private static final Map<String, ExportFile> EXPORT_STORE = new ConcurrentHashMap<>();

    @GetMapping("/types")
    public Map<String, Object> types() {
        List<Map<String, Object>> groups = new ArrayList<>();

        Map<String, Object> followup = new LinkedHashMap<>();
        followup.put("id", "followup");
        followup.put("title", "随访相关报表");
        followup.put("open", true);
        followup.put("reports", buildFollowupReports());
        groups.add(followup);

        Map<String, Object> risk = new LinkedHashMap<>();
        risk.put("id", "risk");
        risk.put("title", "风险分层报表");
        risk.put("open", false);
        risk.put("reports", buildRiskReports());
        groups.add(risk);

        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("id", "alert");
        alert.put("title", "告警相关报表");
        alert.put("open", false);
        alert.put("reports", buildAlertReports());
        groups.add(alert);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", groups);
        return result;
    }

    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam(required = false) String reportId,
                                       @RequestParam(required = false) String timeRange,
                                       @RequestParam(required = false) String dept) {
        Map<String, Object> result = new HashMap<>();
        if (reportId == null || reportId.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "reportId 不能为空");
            return result;
        }

        Map<String, Object> report = buildReportById(reportId.trim(), timeRange, dept);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", report);
        return result;
    }

    @GetMapping("/month-summary")
    public Map<String, Object> monthSummary(@RequestParam(required = false) Integer days) {
        int d = days == null ? 180 : Math.max(30, Math.min(days, 3650));

        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(d);

        List<RiskLevelTrendPoint> riskTrend = dashboardMapper.riskLevelTrendByMonth(startAt, endAt);
        List<DashboardTrendPoint> alertTrend = dashboardMapper.alertTrendByMonth(startAt, endAt);
        List<DashboardTrendPoint> followTrend = dashboardMapper.followUpTaskTrendByMonth(startAt, endAt);

        YearMonth ymStart = YearMonth.from(startAt.toLocalDate());
        YearMonth ymEnd = YearMonth.from(endAt.minusDays(1).toLocalDate());

        LinkedHashMap<String, Map<String, Object>> byMonth = new LinkedHashMap<>();
        YearMonth cur = ymStart;
        while (!cur.isAfter(ymEnd)) {
            String key = cur.toString();
            byMonth.put(key, initRow(key));
            cur = cur.plusMonths(1);
        }

        boolean hasAnyRiskData = false;
        if (riskTrend != null) {
            for (RiskLevelTrendPoint p : riskTrend) {
                if (p == null || p.getDay() == null) {
                    continue;
                }
                Map<String, Object> row = byMonth.computeIfAbsent(p.getDay(), k -> initRow(p.getDay()));
                String level = p.getRiskLevel() == null ? "" : p.getRiskLevel().trim().toUpperCase();
                Long cnt = p.getCount() == null ? 0L : p.getCount();
                if ("HIGH".equals(level)) {
                    row.put("highRisk", cnt);
                    if (cnt != null && cnt > 0) hasAnyRiskData = true;
                } else if ("MID".equals(level) || "MEDIUM".equals(level)) {
                    row.put("midRisk", cnt);
                    if (cnt != null && cnt > 0) hasAnyRiskData = true;
                } else if ("LOW".equals(level)) {
                    row.put("lowRisk", cnt);
                    if (cnt != null && cnt > 0) hasAnyRiskData = true;
                }
            }
        }

        // 兜底：如果区间内 risk_level_history 没有记录，趋势会全部为 0。
        // 为了让首页至少能展示当前风险分层分布，将最新月份填入“当前分布”。
        if (!hasAnyRiskData) {
            List<RiskLevelCount> latest = dashboardMapper.countPatientsByEffectiveRiskLevel();
            long high = 0L;
            long mid = 0L;
            long low = 0L;
            if (latest != null) {
                for (RiskLevelCount c : latest) {
                    if (c == null) continue;
                    String level = c.getRiskLevel() == null ? "" : c.getRiskLevel().trim().toUpperCase();
                    long cnt = c.getCount() == null ? 0L : c.getCount();
                    if ("HIGH".equals(level)) {
                        high = cnt;
                    } else if ("MID".equals(level) || "MEDIUM".equals(level)) {
                        mid = cnt;
                    } else if ("LOW".equals(level)) {
                        low = cnt;
                    }
                }
            }

            String latestMonth = ymEnd.toString();
            Map<String, Object> row = byMonth.computeIfAbsent(latestMonth, k -> initRow(latestMonth));
            row.put("highRisk", high);
            row.put("midRisk", mid);
            row.put("lowRisk", low);
        }

        if (alertTrend != null) {
            for (DashboardTrendPoint p : alertTrend) {
                if (p == null || p.getDay() == null) {
                    continue;
                }
                Map<String, Object> row = byMonth.computeIfAbsent(p.getDay(), k -> initRow(p.getDay()));
                row.put("alerts", p.getValue() == null ? 0L : p.getValue());
            }
        }

        if (followTrend != null) {
            for (DashboardTrendPoint p : followTrend) {
                if (p == null || p.getDay() == null) {
                    continue;
                }
                Map<String, Object> row = byMonth.computeIfAbsent(p.getDay(), k -> initRow(p.getDay()));
                row.put("followups", p.getValue() == null ? 0L : p.getValue());
            }
        }

        List<Map<String, Object>> list = new ArrayList<>(byMonth.values());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @GetMapping("/board")
    public Map<String, Object> board(@RequestParam(required = false) Integer days) {
        int d = days == null ? 180 : Math.max(30, Math.min(days, 3650));

        Map<String, Object> summaryResult = dashboardService.summary(0);
        Object summaryObj = summaryResult == null ? null : summaryResult.get("data");

        Long patientTotal = 0L;
        Long latestHighRisk = 0L;
        Long latestMidRisk = 0L;
        Long latestLowRisk = 0L;

        if (summaryObj instanceof DashboardSummary) {
            DashboardSummary s = (DashboardSummary) summaryObj;
            patientTotal = s.getPatientTotal() == null ? 0L : s.getPatientTotal();
            if (s.getRiskLevel() != null) {
                latestHighRisk = s.getRiskLevel().getHigh() == null ? 0L : s.getRiskLevel().getHigh();
                latestMidRisk = s.getRiskLevel().getMid() == null ? 0L : s.getRiskLevel().getMid();
                latestLowRisk = s.getRiskLevel().getLow() == null ? 0L : s.getRiskLevel().getLow();
            }
        }

        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        LocalDateTime thisMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime lastMonthStart = currentMonth.minusMonths(1).atDay(1).atStartOfDay();

        Long latestNewPatients = dashboardMapper.countNewPatientsInRange(thisMonthStart, nextMonthStart);
        Long latestAlerts = dashboardMapper.countAllAlertsInRange(thisMonthStart, nextMonthStart);
        Long latestFollowups = dashboardMapper.countFollowUpTasksInRange(thisMonthStart, nextMonthStart);

        Long lastMonthAlerts = dashboardMapper.countAllAlertsInRange(lastMonthStart, thisMonthStart);
        double alertChangeRate = 0.0;
        if (lastMonthAlerts != null && lastMonthAlerts > 0) {
            long cur = latestAlerts == null ? 0L : latestAlerts;
            alertChangeRate = (cur - lastMonthAlerts) * 1.0 / lastMonthAlerts;
        }

        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(d);

        List<DashboardTrendPoint> alertTrend = dashboardMapper.alertTrendByMonth(startAt, endAt);
        List<DashboardTrendPoint> followTrend = dashboardMapper.followUpTaskTrendByMonth(startAt, endAt);
        List<RiskLevelTrendPoint> riskTrend = dashboardMapper.riskLevelTrendByMonth(startAt, endAt);

        YearMonth ymStart = YearMonth.from(startAt.toLocalDate());
        YearMonth ymEnd = YearMonth.from(endAt.minusDays(1).toLocalDate());

        // 预生成区间内所有月份，避免趋势表没数据导致 months 为空，前端图表无法展示
        LinkedHashMap<String, long[]> byMonth = new LinkedHashMap<>();
        YearMonth cur = ymStart;
        while (!cur.isAfter(ymEnd)) {
            byMonth.put(cur.toString(), new long[]{0, 0, 0, 0, 0});
            cur = cur.plusMonths(1);
        }

        if (alertTrend != null) {
            for (DashboardTrendPoint p : alertTrend) {
                if (p == null || p.getDay() == null) {
                    continue;
                }
                byMonth.computeIfAbsent(p.getDay(), k -> new long[]{0, 0, 0, 0, 0});
                byMonth.get(p.getDay())[0] = p.getValue() == null ? 0L : p.getValue();
            }
        }
        if (followTrend != null) {
            for (DashboardTrendPoint p : followTrend) {
                if (p == null || p.getDay() == null) {
                    continue;
                }
                byMonth.computeIfAbsent(p.getDay(), k -> new long[]{0, 0, 0, 0, 0});
                byMonth.get(p.getDay())[1] = p.getValue() == null ? 0L : p.getValue();
            }
        }
        boolean hasAnyRiskTrend = false;
        if (riskTrend != null) {
            for (RiskLevelTrendPoint p : riskTrend) {
                if (p == null || p.getDay() == null) {
                    continue;
                }
                byMonth.computeIfAbsent(p.getDay(), k -> new long[]{0, 0, 0, 0, 0});
                String level = p.getRiskLevel() == null ? "" : p.getRiskLevel().trim().toUpperCase();
                long cnt = p.getCount() == null ? 0L : p.getCount();
                if ("HIGH".equals(level)) {
                    byMonth.get(p.getDay())[2] = cnt;
                    if (cnt > 0) hasAnyRiskTrend = true;
                } else if ("MID".equals(level) || "MEDIUM".equals(level)) {
                    byMonth.get(p.getDay())[3] = cnt;
                    if (cnt > 0) hasAnyRiskTrend = true;
                } else if ("LOW".equals(level)) {
                    byMonth.get(p.getDay())[4] = cnt;
                    if (cnt > 0) hasAnyRiskTrend = true;
                }
            }
        }

        // 兜底：如果区间内 risk_level_history 没有记录（或全为 0），
        // 将当前风险分层分布填入最新月份，保证“高危/其他趋势”至少有可见数据。
        if (!hasAnyRiskTrend) {
            List<RiskLevelCount> latest = dashboardMapper.countPatientsByEffectiveRiskLevel();
            long high = 0L;
            long mid = 0L;
            long low = 0L;
            if (latest != null) {
                for (RiskLevelCount c : latest) {
                    if (c == null) continue;
                    String level = c.getRiskLevel() == null ? "" : c.getRiskLevel().trim().toUpperCase();
                    long cnt = c.getCount() == null ? 0L : c.getCount();
                    if ("HIGH".equals(level)) {
                        high = cnt;
                    } else if ("MID".equals(level) || "MEDIUM".equals(level)) {
                        mid = cnt;
                    } else if ("LOW".equals(level)) {
                        low = cnt;
                    }
                }
            }
            String latestMonth = ymEnd.toString();
            byMonth.computeIfAbsent(latestMonth, k -> new long[]{0, 0, 0, 0, 0});
            byMonth.get(latestMonth)[2] = high;
            byMonth.get(latestMonth)[3] = mid;
            byMonth.get(latestMonth)[4] = low;
        }

        List<String> months = new ArrayList<>(byMonth.keySet());
        months.sort(String::compareTo);

        List<Long> alertsArr = new ArrayList<>();
        List<Long> followArr = new ArrayList<>();
        List<Long> highArr = new ArrayList<>();
        List<Long> otherArr = new ArrayList<>();
        for (String m : months) {
            long[] v = byMonth.get(m);
            alertsArr.add(v[0]);
            followArr.add(v[1]);
            highArr.add(v[2]);
            otherArr.add(v[3] + v[4]);
        }

        List<Map<String, Object>> diseaseAnalysis = new ArrayList<>();
        List<Map<String, Object>> rawDisease = dashboardMapper.diseaseAnalysisBySyndrome(startAt, endAt, 20);
        if (rawDisease != null) {
            for (Map<String, Object> r : rawDisease) {
                if (r == null) {
                    continue;
                }
                String disease = r.get("disease") == null ? "" : String.valueOf(r.get("disease"));
                long patientCount = r.get("patientCount") instanceof Number ? ((Number) r.get("patientCount")).longValue() : 0L;
                long stableCnt = r.get("stableCnt") instanceof Number ? ((Number) r.get("stableCnt")).longValue() : 0L;
                long deteriorCnt = r.get("deteriorCnt") instanceof Number ? ((Number) r.get("deteriorCnt")).longValue() : 0L;
                long totalCnt = r.get("totalCnt") instanceof Number ? ((Number) r.get("totalCnt")).longValue() : 0L;

                double stableRate = totalCnt <= 0 ? 0.0 : stableCnt * 1.0 / totalCnt;
                double deteriorationRate = totalCnt <= 0 ? 0.0 : deteriorCnt * 1.0 / totalCnt;
                double improvementRate = totalCnt <= 0 ? 0.0 : Math.max(0.0, 1.0 - stableRate - deteriorationRate);

                Map<String, Object> row = new HashMap<>();
                row.put("disease", disease);
                row.put("patientCount", patientCount);
                row.put("stableRate", stableRate);
                row.put("improvementRate", improvementRate);
                row.put("deteriorationRate", deteriorationRate);
                row.put("carePlanEffectiveness", new ArrayList<>());
                row.put("deteriorationFactors", new ArrayList<>());
                row.put("typicalProfiles", new ArrayList<>());
                row.put("recommendations", new ArrayList<>());
                diseaseAnalysis.add(row);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("latestMonth", currentMonth.toString());
        data.put("latestNewPatients", latestNewPatients == null ? 0L : latestNewPatients);
        data.put("latestAlerts", latestAlerts == null ? 0L : latestAlerts);
        data.put("latestFollowups", latestFollowups == null ? 0L : latestFollowups);
        data.put("latestHighRisk", latestHighRisk);
        data.put("latestMidRisk", latestMidRisk);
        data.put("latestLowRisk", latestLowRisk);
        data.put("latestAuc", 0.0);
        data.put("latestF1", 0.0);
        data.put("alertChangeRate", alertChangeRate);
        data.put("months", months);
        data.put("alertsArr", alertsArr);
        data.put("followArr", followArr);
        data.put("highArr", highArr);
        data.put("otherArr", otherArr);
        data.put("diseaseAnalysis", diseaseAnalysis);
        data.put("patientTotal", patientTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/export")
    public Map<String, Object> export(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        String reportId = body.get("reportId") == null ? null : String.valueOf(body.get("reportId"));
        if (reportId == null || reportId.trim().isEmpty()) {
            // 兼容旧字段：reportType
            reportId = body.get("reportType") == null ? null : String.valueOf(body.get("reportType"));
        }
        if (reportId == null || reportId.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "reportId 不能为空");
            return result;
        }

        String timeRange = body.get("timeRange") == null ? null : String.valueOf(body.get("timeRange"));
        String dept = body.get("dept") == null ? null : String.valueOf(body.get("dept"));
        String format = body.get("format") == null ? null : String.valueOf(body.get("format"));

        if (format != null && !format.trim().isEmpty()) {
            String f = format.trim().toUpperCase();
            if (!"CSV".equals(f)) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "当前仅支持 CSV 导出");
                return result;
            }
        }

        Map<String, Object> report = buildReportById(reportId.trim(), timeRange, dept);
        @SuppressWarnings("unchecked")
        List<String> columns = report.get("columns") instanceof List ? (List<String>) report.get("columns") : Collections.emptyList();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = report.get("rows") instanceof List ? (List<Map<String, Object>>) report.get("rows") : Collections.emptyList();

        byte[] bytes = buildCsv(columns, rows).getBytes(StandardCharsets.UTF_8);
        String fileName = reportId.trim() + ".csv";
        MediaType mediaType = new MediaType("text", "csv", StandardCharsets.UTF_8);
        String id = UUID.randomUUID().toString().replace("-", "");
        EXPORT_STORE.put(id, new ExportFile(bytes, fileName, mediaType));

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", fileName);
        data.put("downloadUrl", "/api/report/download/" + id);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "导出成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable("id") String id) {
        ExportFile file = id == null ? null : EXPORT_STORE.get(id);
        if (file == null || file.bytes == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(file.mediaType == null ? new MediaType("application", "octet-stream") : file.mediaType);
        String name = file.fileName == null ? "report.csv" : file.fileName;
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name);
        return ResponseEntity.ok().headers(headers).body(file.bytes);
    }

    private Map<String, Object> initRow(String day) {
        Map<String, Object> row = new HashMap<>();
        row.put("month", day);
        row.put("highRisk", 0L);
        row.put("midRisk", 0L);
        row.put("lowRisk", 0L);
        row.put("alerts", 0L);
        row.put("followups", 0L);
        return row;
    }

    private List<Map<String, Object>> buildFollowupReports() {
        List<Map<String, Object>> reports = new ArrayList<>();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "followup_task_trend_month");
        report.put("name", "随访任务趋势（按月）");
        report.put("desc", "统计区间内每月随访任务创建数量（按创建时间 created_at）。");
        List<String> columns = new ArrayList<>();
        columns.add("月份");
        columns.add("计划随访数");
        report.put("columns", columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(180);
        List<DashboardTrendPoint> trend = dashboardMapper.followUpTaskTrendByMonth(startAt, endAt);
        if (trend != null) {
            for (DashboardTrendPoint p : trend) {
                if (p == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("月份", p.getDay());
                row.put("计划随访数", p.getValue() == null ? 0L : p.getValue());
                // 兼容前端 Export.vue 的 mapping（计划随访数 -> row.plan）
                row.put("plan", p.getValue() == null ? 0L : p.getValue());
                rows.add(row);
            }
        }
        report.put("rows", rows);
        reports.add(report);
        return reports;
    }

    private List<Map<String, Object>> buildRiskReports() {
        List<Map<String, Object>> reports = new ArrayList<>();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "risk_distribution");
        report.put("name", "风险分层分布");
        report.put("desc", "统计当前患者风险分层人数。");
        List<String> columns = new ArrayList<>();
        columns.add("风险等级");
        columns.add("人数");
        report.put("columns", columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<RiskLevelCount> list = dashboardMapper.countPatientsByEffectiveRiskLevel();
        if (list != null) {
            for (RiskLevelCount c : list) {
                if (c == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("风险等级", c.getRiskLevel());
                row.put("人数", c.getCount() == null ? 0L : c.getCount());
                rows.add(row);
            }
        }
        report.put("rows", rows);
        reports.add(report);
        return reports;
    }

    private List<Map<String, Object>> buildAlertReports() {
        List<Map<String, Object>> reports = new ArrayList<>();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "alert_trend_month");
        report.put("name", "告警趋势（按月）");
        report.put("desc", "统计区间内每月告警数量（health_alert + device_alert）。");
        List<String> columns = new ArrayList<>();
        columns.add("月份");
        columns.add("告警数");
        report.put("columns", columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(180);
        List<DashboardTrendPoint> trend = dashboardMapper.alertTrendByMonth(startAt, endAt);
        if (trend != null) {
            for (DashboardTrendPoint p : trend) {
                if (p == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("月份", p.getDay());
                row.put("告警数", p.getValue() == null ? 0L : p.getValue());
                rows.add(row);
            }
        }
        report.put("rows", rows);
        reports.add(report);
        return reports;
    }

    private Map<String, Object> buildReportById(String reportId, String timeRange, String dept) {
        // timeRange：week/month/quarter/year
        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime startAt = endAt.minusDays(180);
        if (timeRange != null) {
            String tr = timeRange.trim().toLowerCase();
            if ("week".equals(tr)) {
                startAt = endAt.minusDays(7);
            } else if ("month".equals(tr)) {
                startAt = endAt.minusDays(30);
            } else if ("quarter".equals(tr)) {
                startAt = endAt.minusDays(90);
            } else if ("year".equals(tr)) {
                startAt = endAt.minusDays(365);
            }
        }

        String deptName = normalizeDeptName(dept);
        boolean byDay = isByDay(timeRange);

        if ("followup_task_trend_month".equalsIgnoreCase(reportId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", reportId);
            item.put("name", "随访任务趋势（按月）");
            item.put("desc", "统计区间内每月随访任务创建数量（按创建时间 created_at）。");
            List<String> columns = new ArrayList<>();
            columns.add(byDay ? "日期" : "月份");
            columns.add("计划随访数");
            item.put("columns", columns);
            List<Map<String, Object>> rows = new ArrayList<>();
            List<DashboardTrendPoint> trend = byDay
                    ? dashboardMapper.followUpTaskTrendByDayWithDept(startAt, endAt, deptName)
                    : dashboardMapper.followUpTaskTrendByMonthWithDept(startAt, endAt, deptName);
            if (trend != null) {
                for (DashboardTrendPoint p : trend) {
                    if (p == null) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put(byDay ? "日期" : "月份", p.getDay());
                    row.put("计划随访数", p.getValue() == null ? 0L : p.getValue());
                    row.put("plan", p.getValue() == null ? 0L : p.getValue());
                    rows.add(row);
                }
            }
            item.put("rows", rows);
            return item;
        }

        if ("risk_distribution".equalsIgnoreCase(reportId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", reportId);
            item.put("name", "风险分层分布");
            item.put("desc", "统计当前患者风险分层人数。");
            List<String> columns = new ArrayList<>();
            columns.add("风险等级");
            columns.add("人数");
            item.put("columns", columns);
            List<Map<String, Object>> rows = new ArrayList<>();
            List<RiskLevelCount> list = (deptName == null)
                    ? dashboardMapper.countPatientsByEffectiveRiskLevel()
                    : dashboardMapper.countPatientsByEffectiveRiskLevelWithDept(deptName);
            if (list != null) {
                for (RiskLevelCount c : list) {
                    if (c == null) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("风险等级", c.getRiskLevel());
                    row.put("人数", c.getCount() == null ? 0L : c.getCount());
                    rows.add(row);
                }
            }
            item.put("rows", rows);
            return item;
        }

        if ("alert_trend_month".equalsIgnoreCase(reportId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", reportId);
            item.put("name", "告警趋势（按月）");
            item.put("desc", "统计区间内每月告警数量（health_alert + device_alert）。");
            List<String> columns = new ArrayList<>();
            columns.add(byDay ? "日期" : "月份");
            columns.add("告警数");
            item.put("columns", columns);
            List<Map<String, Object>> rows = new ArrayList<>();
            List<DashboardTrendPoint> trend = byDay
                    ? dashboardMapper.alertTrendByDayWithDept(startAt, endAt, deptName)
                    : dashboardMapper.alertTrendByMonthWithDept(startAt, endAt, deptName);
            if (trend != null) {
                for (DashboardTrendPoint p : trend) {
                    if (p == null) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put(byDay ? "日期" : "月份", p.getDay());
                    row.put("告警数", p.getValue() == null ? 0L : p.getValue());
                    rows.add(row);
                }
            }
            item.put("rows", rows);
            return item;
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", reportId);
        item.put("name", reportId);
        item.put("desc", "未知报表类型");
        item.put("columns", Collections.emptyList());
        item.put("rows", Collections.emptyList());
        return item;
    }

    private static boolean isByDay(String timeRange) {
        if (timeRange == null) {
            return false;
        }
        String tr = timeRange.trim().toLowerCase();
        return "week".equals(tr) || "month".equals(tr);
    }

    private static String normalizeDeptName(String dept) {
        if (dept == null) {
            return null;
        }
        String d = dept.trim();
        if (d.isEmpty() || "ALL".equalsIgnoreCase(d)) {
            return null;
        }
        // 前端下拉是固定 code，这里映射到后端目前存储在 user.address 的中文科室
        if ("CARD".equalsIgnoreCase(d)) {
            return "心内科";
        }
        if ("ENDO".equalsIgnoreCase(d)) {
            return "内分泌";
        }
        if ("TCM".equalsIgnoreCase(d)) {
            return "中医";
        }
        // 兜底：允许直接传中文或其他自定义科室名，走模糊匹配
        return d;
    }

    private static String buildCsv(List<String> columns, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        if (columns == null || columns.isEmpty()) {
            return "";
        }
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(csvEscape(columns.get(i)));
        }
        sb.append('\n');

        if (rows != null) {
            for (Map<String, Object> r : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) sb.append(',');
                    String col = columns.get(i);
                    Object v = r == null ? null : r.get(col);
                    sb.append(csvEscape(v == null ? "" : String.valueOf(v)));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "";
        }
        String x = s;
        boolean needQuote = x.contains(",") || x.contains("\n") || x.contains("\r") || x.contains("\"");
        x = x.replace("\"", "\"\"");
        return needQuote ? ("\"" + x + "\"") : x;
    }
}
