package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.HealthAlertWithPatient;
import com.example.zhinengsuifang.entity.DeviceAlert;
import com.example.zhinengsuifang.entity.FollowUpTask;
import com.example.zhinengsuifang.entity.HealthAlert;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.AlertCenterMapper;
import com.example.zhinengsuifang.mapper.DeviceAlertMapper;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.HealthAlertMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.HealthAlertService;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/alert")
@Tag(name = "告警", description = "告警相关接口（首页与列表）")
public class ApiAlertController {

    @Resource
    private HealthAlertService healthAlertService;

    @Resource
    private AlertCenterMapper alertCenterMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private HealthAlertMapper healthAlertMapper;

    @Resource
    private DeviceAlertMapper deviceAlertMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未授权或无权限");
            return result;
        }

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        String source = body.get("source") == null ? null : String.valueOf(body.get("source"));
        Long patientId = toLong(body.get("patientId"));
        if (source == null || source.trim().isEmpty() || patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "source / patientId 不能为空");
            return result;
        }

        User patient = userMapper.findPatientById(patientId);
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "patientId 不存在");
            return result;
        }

        String src = source.trim().toUpperCase();
        String severity = body.get("severity") == null ? null : String.valueOf(body.get("severity"));
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        severity = (severity == null || severity.trim().isEmpty()) ? "WARN" : severity.trim().toUpperCase();
        status = (status == null || status.trim().isEmpty()) ? "NEW" : status.trim().toUpperCase();

        Long newId;
        if ("DEVICE".equals(src)) {
            DeviceAlert alert = new DeviceAlert();
            alert.setPatientId(patientId);
            alert.setDeviceSn(body.get("deviceSn") == null ? null : String.valueOf(body.get("deviceSn")));
            alert.setAlertCode(body.get("alertCode") == null ? null : String.valueOf(body.get("alertCode")));
            alert.setAlertMessage(body.get("alertMessage") == null ? null : String.valueOf(body.get("alertMessage")));
            alert.setSeverity(severity);
            alert.setStatus(status);
            LocalDateTime occurredAt = parseDateTime(body.get("occurredAt"));
            alert.setOccurredAt(occurredAt == null ? LocalDateTime.now() : occurredAt);
            deviceAlertMapper.insert(alert);
            newId = alert.getId();
        } else {
            HealthAlert alert = new HealthAlert();
            alert.setPatientId(patientId);
            alert.setMetricType(body.get("metricType") == null ? null : String.valueOf(body.get("metricType")));
            alert.setPrevValue1(toDouble(body.get("prevValue1")));
            alert.setPrevValue2(toDouble(body.get("prevValue2")));
            alert.setCurrValue1(toDouble(body.get("currValue1")));
            alert.setCurrValue2(toDouble(body.get("currValue2")));
            alert.setDeltaValue1(toDouble(body.get("deltaValue1")));
            alert.setDeltaValue2(toDouble(body.get("deltaValue2")));
            alert.setSeverity(severity);
            alert.setStatus(status);
            healthAlertMapper.insert(alert);
            newId = alert.getId();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("source", src);
        data.put("id", newId);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "新增成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String doctor,
                                      @RequestParam(required = false) String range,
                                      @RequestParam(required = false) String alertId) {
        List<Map<String, Object>> raw = alertCenterMapper.selectAlertCenterPage(
                null,
                null,
                null,
                null,
                "HEALTH",
                null,
                null,
                0,
                2000
        );

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> typeList = new LinkedHashSet<>();
        Set<String> doctorList = new LinkedHashSet<>();

        if (raw != null) {
            for (Map<String, Object> r : raw) {
                if (r == null) {
                    continue;
                }
                Long pid = toLong(r.get("patientId"));
                String metricType = firstToken(r.get("summary"));
                String alertTypeText = mapPatientAlertType(metricType);
                String doctorName = pid == null ? "" : safeStr(userMapper.findPatientResponsibleDoctorName(pid));

                Map<String, Object> row = new HashMap<>();
                row.put("id", toLong(r.get("alertId")));
                row.put("patientId", pid);
                row.put("patientName", safeStr(r.get("patientName")));
                row.put("severityText", mapSeverityText(safeStr(r.get("level"))));
                row.put("alertType", alertTypeText);
                row.put("firstTime", formatDateTime(r.get("alertTime")));
                row.put("lastTime", formatDateTime(r.get("alertTime")));
                row.put("duration", "");
                row.put("doctor", doctorName);
                row.put("statusText", mapStatusText(safeStr(r.get("status"))));
                row.put("selected", false);
                rows.add(row);

                if (alertTypeText != null && !alertTypeText.isEmpty()) {
                    typeList.add(alertTypeText);
                }
                if (doctorName != null && !doctorName.isEmpty()) {
                    doctorList.add(doctorName);
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("statusParam", status);
        data.put("typeParam", type);
        data.put("doctorParam", doctor);
        data.put("rangeParam", range);
        data.put("selectedAlertId", alertId);
        data.put("typeList", new ArrayList<>(typeList));
        data.put("doctorList", new ArrayList<>(doctorList));
        data.put("rows", rows);
        data.put("detail", null);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/hardware")
    public Map<String, Object> hardware(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String deviceType,
                                        @RequestParam(required = false) String range,
                                        @RequestParam(required = false) String alertId) {
        List<Map<String, Object>> raw = alertCenterMapper.selectAlertCenterPage(
                null,
                null,
                null,
                null,
                "DEVICE",
                null,
                null,
                0,
                2000
        );

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> deviceTypeList = new LinkedHashSet<>();

        if (raw != null) {
            for (Map<String, Object> r : raw) {
                if (r == null) {
                    continue;
                }

                String summary = safeStr(r.get("summary"));
                String sn = firstToken(summary);
                String alertText = restAfterFirstToken(summary);
                String deviceTypeText = (sn == null || sn.isEmpty()) ? "开发板" : "开发板";

                Map<String, Object> row = new HashMap<>();
                row.put("id", toLong(r.get("alertId")));
                row.put("patientId", toLong(r.get("patientId")));
                row.put("patientName", safeStr(r.get("patientName")));
                row.put("severityText", mapSeverityText(safeStr(r.get("level"))));
                row.put("deviceType", deviceTypeText);
                row.put("deviceId", sn);
                row.put("alertType", alertText == null || alertText.isEmpty() ? "设备异常" : alertText);
                row.put("firstTime", formatDateTime(r.get("alertTime")));
                row.put("lastTime", formatDateTime(r.get("alertTime")));
                row.put("duration", "");
                row.put("statusText", mapStatusText(safeStr(r.get("status"))));
                row.put("selected", false);
                rows.add(row);

                if (deviceTypeText != null && !deviceTypeText.isEmpty()) {
                    deviceTypeList.add(deviceTypeText);
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("statusParam", status);
        data.put("deviceTypeParam", deviceType);
        data.put("rangeParam", range);
        data.put("selectedAlertId", alertId);
        data.put("deviceTypeList", new ArrayList<>(deviceTypeList));
        data.put("rows", rows);
        data.put("detail", null);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/home-high-risk-list")
    public Map<String, Object> homeHighRiskList(@RequestParam String doctorUsername,
                                                @RequestParam String doctorPassword,
                                                @RequestParam(required = false) Integer limit) {
        int lim = limit == null ? 10 : Math.max(1, Math.min(limit, 50));

        Map<String, Object> serviceResult = healthAlertService.searchAlertsByPatientName(
                doctorUsername,
                doctorPassword,
                null,
                "NEW",
                lim
        );

        Map<String, Object> result = new HashMap<>();
        if (serviceResult == null) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常");
            return result;
        }

        Object codeObj = serviceResult.get("code");
        Integer code = codeObj instanceof Number ? ((Number) codeObj).intValue() : null;
        if (code == null || !code.equals(ApiCode.SUCCESS.getCode())) {
            return serviceResult;
        }

        Object dataObj = serviceResult.get("data");
        List<Map<String, Object>> rows = new ArrayList<>();
        if (dataObj instanceof List<?>) {
            for (Object o : (List<?>) dataObj) {
                if (!(o instanceof HealthAlertWithPatient)) {
                    continue;
                }
                HealthAlertWithPatient a = (HealthAlertWithPatient) o;
                Map<String, Object> row = new HashMap<>();
                row.put("alertId", a.getId());
                row.put("patientId", a.getPatientId());
                row.put("patientName", a.getPatientName());
                row.put("riskFactors", buildRiskFactors(a));
                row.put("durationText", buildDurationText(a.getCreatedAt()));
                Long patientId = a.getPatientId();
                String doctorName = patientId == null ? "" : safeStr(userMapper.findPatientResponsibleDoctorName(patientId));
                row.put("doctorName", doctorName);
                rows.add(row);
            }
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", rows);
        return result;
    }

    private String buildRiskFactors(HealthAlertWithPatient a) {
        if (a == null) {
            return "";
        }
        String metric = a.getMetricType() == null ? "" : a.getMetricType().trim();
        String severity = a.getSeverity() == null ? "" : a.getSeverity().trim();
        String value = a.getCurrValue1() == null ? "" : String.valueOf(a.getCurrValue1());
        String delta = a.getDeltaValue1() == null ? "" : String.valueOf(a.getDeltaValue1());
        if (metric.isEmpty() && severity.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(metric);
        if (!value.isEmpty()) {
            sb.append("=").append(value);
        }
        if (!delta.isEmpty()) {
            sb.append("(Δ").append(delta).append(")");
        }
        if (!severity.isEmpty()) {
            sb.append(" ").append(severity);
        }
        return sb.toString().trim();
    }

    private String buildDurationText(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }
        Duration d = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(0, d.toMinutes());
        if (minutes < 60) {
            return minutes + "分钟";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时";
        }
        long days = hours / 24;
        return days + "天";
    }

    /**
     * 更新随访任务信息（保存草稿/修改，不改变状态）
     */
    @PostMapping("/followup/task/update")
    public Map<String, Object> updateFollowupTask(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未授权或无权限");
            return result;
        }

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Object patientIdObj = body.get("patientId");
        Long patientId = patientIdObj instanceof Number ? ((Number) patientIdObj).longValue() : null;
        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "患者ID不能为空");
            return result;
        }

        String followupType = body.get("followupType") == null ? null : String.valueOf(body.get("followupType"));
        String planDate = body.get("planDate") == null ? null : String.valueOf(body.get("planDate"));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        Object staffIdObj = body.get("staffId");
        Long staffId = null;
        if (staffIdObj instanceof Number) {
            staffId = ((Number) staffIdObj).longValue();
        } else if (staffIdObj instanceof String && !((String) staffIdObj).isEmpty()) {
            try {
                staffId = Long.parseLong((String) staffIdObj);
            } catch (NumberFormatException ignored) {}
        }

        // checklist 转 JSON 字符串
        String checklistJson = null;
        Object checklistObj = body.get("checklist");
        if (checklistObj instanceof List) {
            checklistJson = checklistObj.toString();
        }

        // 查找该患者的待处理任务（兼容历史状态：ASSIGNED/PENDING/IN_PROGRESS）
        FollowUpTask task = followUpTaskMapper.findLatestActiveByPatientId(patientId);
        if (task == null) {
            // 没有待处理任务，创建新任务
            task = new FollowUpTask();
            task.setPatientId(patientId);
            task.setDoctorId(operator.getId());
            task.setFollowUpUserId(staffId);
            task.setTriggerType(followupType);
            task.setDescription(content);
            task.setStatus("PENDING");
            task.setExt1(planDate);
            task.setExt2(checklistJson);
            followUpTaskMapper.insert(task);
        } else {
            // 更新现有任务
            followUpTaskMapper.updateTaskInfo(task.getId(), followupType, content, staffId, planDate, checklistJson);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "保存成功");
        return result;
    }

    /**
     * 重新下发随访任务（取消旧任务 + 创建新任务）
     */
    @PostMapping("/followup/task/resend")
    public Map<String, Object> resendFollowupTask(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未授权或无权限");
            return result;
        }

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Object patientIdObj = body.get("patientId");
        Long patientId = patientIdObj instanceof Number ? ((Number) patientIdObj).longValue() : null;
        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "患者ID不能为空");
            return result;
        }

        String followupType = body.get("followupType") == null ? null : String.valueOf(body.get("followupType"));
        String planDate = body.get("planDate") == null ? null : String.valueOf(body.get("planDate"));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        Object staffIdObj = body.get("staffId");
        Long staffId = null;
        if (staffIdObj instanceof Number) {
            staffId = ((Number) staffIdObj).longValue();
        } else if (staffIdObj instanceof String && !((String) staffIdObj).isEmpty()) {
            try {
                staffId = Long.parseLong((String) staffIdObj);
            } catch (NumberFormatException ignored) {}
        }

        String checklistJson = null;
        Object checklistObj = body.get("checklist");
        if (checklistObj instanceof List) {
            checklistJson = checklistObj.toString();
        }

        // 1. 取消该患者所有待处理的任务（兼容历史状态：ASSIGNED/PENDING/IN_PROGRESS）
        FollowUpTask oldTask = followUpTaskMapper.findLatestActiveByPatientId(patientId);
        if (oldTask != null) {
            followUpTaskMapper.updateStatus(oldTask.getId(), "CANCELLED");
        }

        // 2. 创建新任务
        FollowUpTask newTask = new FollowUpTask();
        newTask.setPatientId(patientId);
        newTask.setDoctorId(operator.getId());
        newTask.setFollowUpUserId(staffId);
        newTask.setTriggerType(followupType);
        newTask.setDescription(content);
        newTask.setStatus("PENDING");
        newTask.setExt1(planDate);
        newTask.setExt2(checklistJson);
        followUpTaskMapper.insert(newTask);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "任务已重新下发");
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", newTask.getId());
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

    private static Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDateTime) {
            return (LocalDateTime) o;
        }
        String s = String.valueOf(o);
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.contains("T")) {
                s = s.replace('T', ' ');
            }
            if (s.length() == 16) {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            if (s.length() == 19) {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(String.valueOf(o));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String mapSeverityText(String severity) {
        if (severity == null) {
            return "";
        }
        String s = severity.trim().toUpperCase();
        if (s.contains("RED") || s.contains("HIGH") || s.contains("3")) {
            return "红色";
        }
        if (s.contains("WARN") || s.contains("MID") || s.contains("MED") || s.contains("2")) {
            return "黄色";
        }
        if (s.contains("LOW") || s.contains("GREEN") || s.contains("1")) {
            return "绿色";
        }
        if (s.contains("高")) {
            return "红色";
        }
        if (s.contains("中")) {
            return "黄色";
        }
        if (s.contains("低")) {
            return "绿色";
        }
        return severity;
    }

    private static String mapStatusText(String status) {
        if (status == null) {
            return "";
        }
        String s = status.trim().toUpperCase();
        if (s.isEmpty() || s.equals("NEW")) {
            return "未处理";
        }
        if (s.contains("IN_PROGRESS") || s.contains("PROCESS") || s.contains("FOLLOWUP_CREATED")) {
            return "处理中";
        }
        if (s.contains("CLOSED") || s.contains("REVIEWED") || s.contains("IGNORED") || s.contains("DONE") || s.contains("COMPLETED")) {
            return "已关闭";
        }
        return status;
    }

    private static String mapPatientAlertType(String metricType) {
        if (metricType == null) {
            return "患者异常";
        }
        String t = metricType.trim().toUpperCase();
        if (t.isEmpty()) {
            return "患者异常";
        }
        if (t.equals("BP") || t.contains("SBP") || t.contains("DBP") || t.contains("PRESS")) {
            return "血压异常";
        }
        if (t.equals("HR") || t.contains("HEART")) {
            return "心率异常";
        }
        if (t.equals("SPO2") || t.contains("O2")) {
            return "血氧异常";
        }
        if (t.contains("TEMP")) {
            return "体温异常";
        }
        if (t.contains("WEIGHT")) {
            return "体重异常";
        }
        if (t.contains("GLUCOSE")) {
            return "血糖异常";
        }
        return "患者异常";
    }

    private static String firstToken(Object summary) {
        String s = safeStr(summary).trim();
        if (s.isEmpty()) {
            return "";
        }
        int idx = s.indexOf(' ');
        if (idx < 0) {
            return s;
        }
        return s.substring(0, idx).trim();
    }

    private static String restAfterFirstToken(Object summary) {
        String s = safeStr(summary).trim();
        if (s.isEmpty()) {
            return "";
        }
        int idx = s.indexOf(' ');
        if (idx < 0) {
            return "";
        }
        return s.substring(idx + 1).trim();
    }

    private static String formatDateTime(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof LocalDateTime) {
            return ((LocalDateTime) o).toString().replace('T', ' ');
        }
        return String.valueOf(o).replace('T', ' ');
    }
}
