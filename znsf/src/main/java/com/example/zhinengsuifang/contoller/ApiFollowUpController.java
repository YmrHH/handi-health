package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.FollowUpUserTaskStatusRequest;
import com.example.zhinengsuifang.dto.RescheduleFollowUpTaskRequest;
import com.example.zhinengsuifang.entity.FollowUpTask;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.FollowUpScheduleMapper;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.FollowUpWorkbenchMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupRecordMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/followup")
@Tag(name = "随访工作台", description = "随访工作台相关接口")
public class ApiFollowUpController {

    @Resource
    private FollowUpWorkbenchMapper followUpWorkbenchMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private FollowUpScheduleMapper followUpScheduleMapper;

    @Resource
    private PatientFollowupRecordMapper patientFollowupRecordMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/list")
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(required = false) Integer pageNo,
                                    @RequestParam(required = false) Integer pageSize,
                                    @RequestParam(required = false) String riskLevel,
                                    @RequestParam(required = false) String followupType,
                                    @RequestParam(required = false) String resultStatus,
                                    @RequestParam(required = false) String staffName,
                                    @RequestParam(required = false) String patientName,
                                    @RequestParam(required = false) Long staffId,
                                    @RequestParam(required = false) Long doctorId,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();

        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }

        String role = operator.getRole() == null ? "" : operator.getRole().trim().toUpperCase();
        if ("DOCTOR".equals(role) && doctorId == null) {
            doctorId = operator.getId();
        } else if ("FOLLOW_UP".equals(role) && staffId == null) {
            staffId = operator.getId();
        }

        int p = pageNo == null ? 1 : Math.max(1, pageNo);
        int ps = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 200));
        int offset = (p - 1) * ps;
        LocalDateTime startAt = parseDateStart(startDate);
        LocalDateTime endAtValue = parseDateEnd(endDate);

        Long total = patientFollowupRecordMapper.countFollowupRecordPage(doctorId, staffId, startAt, endAtValue, riskLevel, followupType, resultStatus, patientName, staffName);
        List<Map<String, Object>> raw = patientFollowupRecordMapper.selectFollowupRecordPage(doctorId, staffId, startAt, endAtValue, riskLevel, followupType, resultStatus, patientName, staffName, offset, ps);

        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> r : raw) {
                if (r == null) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", r.get("id"));
                row.put("patientId", r.get("patientId"));
                row.put("followupDate", formatDateTime(r.get("followupTime")));
                row.put("patientName", r.get("patientName"));
                row.put("riskLevel", r.get("riskLevel"));
                row.put("followupType", r.get("followupType"));
                row.put("staffName", r.get("staffName"));
                row.put("contentSummary", r.get("contentSummary"));
                String resultStatusCode = firstNonBlank(r.get("resultStatus"));
                row.put("resultStatusCode", resultStatusCode);
                row.put("resultStatus", mapFollowupResultText(resultStatusCode));
                row.put("nextPlanTime", formatDateTime(r.get("nextFollowupDate")));
                rows.add(row);
            }
        }

        int totalPages = (int) Math.ceil((total == null ? 0 : total) * 1.0 / ps);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("pageNo", p);
        data.put("totalPages", totalPages);
        data.put("total", total == null ? 0L : total);
        data.put("stats", new HashMap<>());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/detail")
    public Map<String, Object> detail(HttpServletRequest request, @RequestParam(required = false) Long id) {
        Map<String, Object> result = new HashMap<>();

        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }
        if (id == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "id 不能为空");
            return result;
        }

        String role = operator.getRole() == null ? "" : operator.getRole().trim().toUpperCase();
        Map<String, Object> detail = "FOLLOW_UP".equals(role)
                ? patientFollowupRecordMapper.findDetailByIdForStaff(id, operator.getId())
                : patientFollowupRecordMapper.findDetailById(id);
        if (detail == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "随访记录不存在");
            return result;
        }

        Map<String, Object> record = new HashMap<>();
        record.put("id", detail.get("id"));
        record.put("followupTime", formatDateTime(detail.get("followup_time") != null ? detail.get("followup_time") : detail.get("followupTime")));
        record.put("followupType", detail.get("followup_type") != null ? detail.get("followup_type") : detail.get("followupType"));
        record.put("staffName", firstNonBlank(detail.get("staffName"), detail.get("doctorName"), "医生/护士"));
        String resultStatusCode = firstNonBlank(detail.get("result_status"), detail.get("resultStatus"));
        record.put("resultStatusCode", resultStatusCode);
        record.put("resultStatus", mapFollowupResultText(resultStatusCode));
        String riskLevelValue = firstNonBlank(detail.get("risk_level"), detail.get("riskLevel"));
        record.put("riskLevel", riskLevelValue);
        record.put("riskLevelCode", normalizeRiskCode(riskLevelValue));
        record.put("riskLevelText", normalizeRiskText(riskLevelValue));
        record.put("nextPlanTime", formatDateTime(detail.get("next_followup_date") != null ? detail.get("next_followup_date") : detail.get("nextFollowupDate")));
        record.put("contentSummary", firstNonBlank(detail.get("content_summary"), detail.get("contentSummary")));
        record.put("symptomChange", firstNonBlank(detail.get("symptom_change"), detail.get("symptomChange")));
        record.put("sbp", detail.get("sbp"));
        record.put("dbp", detail.get("dbp"));
        record.put("heartRate", firstNonBlank(detail.get("heart_rate"), detail.get("heartRate")));
        record.put("weight", firstNonBlank(detail.get("weight_kg"), detail.get("weightKg")));
        record.put("vitalMeasureTime", formatDateTime(detail.get("vital_measure_time") != null ? detail.get("vital_measure_time") : detail.get("vitalMeasureTime")));
        record.put("medAdherence", firstNonBlank(detail.get("med_adherence"), detail.get("medAdherence")));
        record.put("medPlanSummary", firstNonBlank(detail.get("med_plan_summary"), detail.get("medPlanSummary")));
        record.put("adverseReaction", firstNonBlank(detail.get("adverse_reaction"), detail.get("adverseReaction")));
        record.put("tcmFace", firstNonBlank(detail.get("tcm_face"), detail.get("tcmFace")));
        record.put("tcmTongueBody", firstNonBlank(detail.get("tcm_tongue_body"), detail.get("tcmTongueBody")));
        record.put("tcmTongueCoating", firstNonBlank(detail.get("tcm_tongue_coating"), detail.get("tcmTongueCoating")));
        record.put("tcmTongueImageUrl", firstNonBlank(detail.get("tcm_tongue_image_url"), detail.get("tcmTongueImageUrl")));
        record.put("tcmPulseRate", firstNonBlank(detail.get("tcm_pulse_rate"), detail.get("tcmPulseRate")));
        record.put("tcmPulseTypes", firstNonBlank(detail.get("tcm_pulse_types"), detail.get("tcmPulseTypes")));
        String tcmConclusion = firstNonBlank(detail.get("tcm_conclusion"), detail.get("tcmConclusion"));
        String tcmRemark = firstNonBlank(detail.get("tcm_remark"), detail.get("tcmRemark"));
        String tcmEvaluationAdvice = mergeUniqueText(tcmRemark, tcmConclusion);
        record.put("tcmConclusion", tcmConclusion);
        record.put("tcmRemark", tcmRemark);
        record.put("tcmEvaluationAdvice", tcmEvaluationAdvice);
        String healthAdvice = firstNonBlank(detail.get("lab_summary"), detail.get("labSummary"));
        record.put("labSummary", healthAdvice);
        record.put("healthAdvice", healthAdvice);
        record.put("summary", firstNonBlank(detail.get("content_summary"), detail.get("contentSummary")));
        record.put("nextFollowupType", firstNonBlank(detail.get("next_followup_type"), detail.get("nextFollowupType")));
        record.put("nextFollowupDate", formatDateTime(detail.get("next_followup_date") != null ? detail.get("next_followup_date") : detail.get("nextFollowupDate")));
        record.put("nextFollowupRemark", firstNonBlank(detail.get("next_followup_remark"), detail.get("nextFollowupRemark")));

        Map<String, Object> patient = new HashMap<>();
        patient.put("name", firstNonBlank(detail.get("patientName"), ""));
        patient.put("gender", firstNonBlank(detail.get("patientGender"), detail.get("patient_gender")));
        patient.put("age", detail.get("patientAge") != null ? detail.get("patientAge") : detail.get("patient_age"));
        patient.put("idCard", firstNonBlank(detail.get("patientIdCard"), detail.get("patient_id_card")));
        patient.put("phone", firstNonBlank(detail.get("patientPhone"), detail.get("patient_phone")));
        patient.put("disease", "");
        patient.put("syndrome", "");

        Map<String, Object> data = new HashMap<>();
        data.put("record", record);
        data.put("patient", patient);
        data.put("tcmPulseTypeList", new ArrayList<>());

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

    private static LocalDateTime parseDateStart(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim()).atStartOfDay();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (Exception ignored) {
        }
        return null;
    }

    private static LocalDateTime parseDateEnd(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusSeconds(1);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String formatDateTime(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).replace('T', ' ');
    }

    private static String normalizeRiskCode(String riskLevel) {
        String s = firstNonBlank(riskLevel, "MID").toUpperCase();
        if (s.contains("HIGH") || s.contains("高")) {
            return "HIGH";
        }
        if (s.contains("LOW") || s.contains("低")) {
            return "LOW";
        }
        return "MID";
    }

    private static String normalizeRiskText(String riskLevel) {
        String code = normalizeRiskCode(riskLevel);
        if ("HIGH".equals(code)) {
            return "高风险";
        }
        if ("LOW".equals(code)) {
            return "低风险";
        }
        return "中风险";
    }

    private static String firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                String s = String.valueOf(value).trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String mergeUniqueText(Object... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        java.util.LinkedHashSet<String> parts = new java.util.LinkedHashSet<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String s = String.valueOf(value).trim();
            if (!s.isEmpty()) {
                parts.add(s);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("；", parts);
    }

    @GetMapping("/tasks")
    public Map<String, Object> tasks(HttpServletRequest request,
                                     @RequestParam(required = false) String doctorUsername,
                                     @RequestParam(required = false) String doctorPassword,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Long staffId,
                                     @RequestParam(required = false) Long doctorId,
                                     @RequestParam(required = false) String riskLevel,
                                     @RequestParam(required = false) LocalDateTime startAt,
                                     @RequestParam(required = false) LocalDateTime endAt) {

        Map<String, Object> result = new HashMap<>();

        // 1) Header 鉴权优先（Bearer <userId>）
        User operator = null;
        Long headerUserId = AuthHeaderUtil.getUserId(request);
        if (headerUserId != null) {
            operator = userMapper.findById(headerUserId);
        }

        if (operator != null) {
            // 仅对部分角色施加默认过滤；其它角色（含空 role）允许按传入条件查询
            String role = operator.getRole() == null ? "" : operator.getRole().trim().toUpperCase();
            if ("DOCTOR".equals(role)) {
                // 医生只看自己的任务
                doctorId = operator.getId();
            } else if ("FOLLOW_UP".equals(role)) {
                // 随访员默认只看自己的任务（除非调用方明确传了 staffId）
                if (staffId == null) {
                    staffId = operator.getId();
                }
            }
        } else {
            // 2) 回退到 doctorUsername/doctorPassword（兼容旧调用）
            if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "未登录或登录已过期");
                return result;
            }

            User doctor = userMapper.findByUsername(doctorUsername.trim());
            if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "無权限");
                return result;
            }

            boolean match = passwordEncoder.matches(doctorPassword, doctor.getPassword());
            if (!match) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "医生账号或密码错誤");
                return result;
            }
            doctorId = doctor.getId();
        }

        if (pageSize != null && (pageSize < 1 || pageSize > 200)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "pageSize 参数不合法，仅支持 1-200");
            return result;
        }

        int p = page == null ? 1 : Math.max(1, page);
        int ps = pageSize == null ? 10 : pageSize;
        int offset = (p - 1) * ps;

        Long total = followUpWorkbenchMapper.countTasks(status, staffId, doctorId, riskLevel, startAt, endAt);
        List<Map<String, Object>> rows = followUpWorkbenchMapper.selectTasks(status, staffId, doctorId, riskLevel, startAt, endAt, offset, ps);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total == null ? 0L : total);
        data.put("rows", rows == null ? Collections.emptyList() : rows);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/task/start")
    public Map<String, Object> start(HttpServletRequest request, @Valid @RequestBody FollowUpUserTaskStatusRequest body) {
        return updateTaskStatusByFollowUpUser(request, body, "IN_PROGRESS");
    }

    @PostMapping("/task/complete")
    public Map<String, Object> complete(HttpServletRequest request, @Valid @RequestBody FollowUpUserTaskStatusRequest body) {
        return updateTaskStatusByFollowUpUser(request, body, "COMPLETED");
    }

    @PostMapping("/task/reschedule")
    public Map<String, Object> reschedule(HttpServletRequest request, @Valid @RequestBody RescheduleFollowUpTaskRequest body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        Long taskId = body.getTaskId();
        String followUpUsername = body.getFollowUpUsername();
        String followUpPassword = body.getFollowUpPassword();
        String dueAtObj = body.getDueAt();
        LocalDateTime dueAt = null;
        if (dueAtObj != null && !dueAtObj.trim().isEmpty()) {
            dueAt = LocalDateTime.parse(dueAtObj.trim());
        }

        if (taskId == null || dueAt == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        User followUpUser = null;
        Long uid = AuthHeaderUtil.getUserId(request);
        if (uid != null) {
            User u = userMapper.findById(uid);
            if (u != null && u.getRole() != null && "FOLLOW_UP".equalsIgnoreCase(u.getRole().trim())) {
                followUpUser = u;
            }
        }

        if (followUpUser == null) {
            if (followUpUsername == null || followUpUsername.trim().isEmpty() || followUpPassword == null || followUpPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "参数错誤");
                return result;
            }

            User u = userMapper.findByUsername(followUpUsername.trim());
            if (u == null || u.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(u.getRole().trim())) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "無权限");
                return result;
            }

            boolean match = passwordEncoder.matches(followUpPassword, u.getPassword());
            if (!match) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "随访员账号或密码错誤");
                return result;
            }
            followUpUser = u;
        }

        FollowUpTask task = followUpTaskMapper.findById(taskId);
        if (task == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "任务不存在");
            return result;
        }

        if (!followUpUser.getId().equals(task.getFollowUpUserId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "只能操作分配給自己的任务");
            return result;
        }

        int updated = followUpScheduleMapper.rescheduleByTaskId(taskId, dueAt, "DUE");

        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", updated > 0 ? "改期成功" : "改期失败");
        return result;
    }

    private Map<String, Object> updateTaskStatusByFollowUpUser(HttpServletRequest request, FollowUpUserTaskStatusRequest body, String newStatus) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        Long taskId = body.getTaskId();
        if (taskId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        // 1) Header 鉴权优先
        User followUpUser = null;
        Long uid = AuthHeaderUtil.getUserId(request);
        if (uid != null) {
            User u = userMapper.findById(uid);
            if (u != null && u.getRole() != null && "FOLLOW_UP".equalsIgnoreCase(u.getRole().trim())) {
                followUpUser = u;
            }
        }

        // 2) 回退到账号密码二次校验
        if (followUpUser == null) {
            String followUpUsername = body.getFollowUpUsername();
            String followUpPassword = body.getFollowUpPassword();
            if (followUpUsername == null || followUpUsername.trim().isEmpty() || followUpPassword == null || followUpPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "参数错誤");
                return result;
            }
            User u = userMapper.findByUsername(followUpUsername.trim());
            if (u == null || u.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(u.getRole().trim())) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "無权限");
                return result;
            }
            boolean match = passwordEncoder.matches(followUpPassword, u.getPassword());
            if (!match) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "随访员账号或密码错誤");
                return result;
            }
            followUpUser = u;
        }

        FollowUpTask task = followUpTaskMapper.findById(taskId);
        if (task == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "任务不存在");
            return result;
        }

        if (!followUpUser.getId().equals(task.getFollowUpUserId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "只能操作分配給自己的任务");
            return result;
        }

        int updated = followUpTaskMapper.updateStatus(taskId, newStatus);
        if (updated > 0 && "COMPLETED".equalsIgnoreCase(newStatus)) {
            followUpScheduleMapper.updateByTaskId(taskId, "COMPLETED", LocalDateTime.now());
        }

        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", updated > 0 ? "更新成功" : "更新失败");
        return result;
    }

    private static String mapFollowupResultText(String value) {
        String s = firstNonBlank(value).trim();
        if (s.isEmpty()) {
            return "";
        }
        String u = s.toUpperCase();
        if (u.contains("COMPLETED") || u.contains("DONE") || u.contains("SUCCESS") || u.contains("FINISHED")) {
            return "已完成";
        }
        if (u.contains("FAILED") || u.contains("FAIL") || u.contains("ERROR")) {
            return "失败";
        }
        if (u.contains("CANCEL")) {
            return "已取消";
        }
        if (u.contains("PENDING") || u.contains("WAIT")) {
            return "待处理";
        }
        if (u.contains("PROCESS") || u.contains("FOLLOW")) {
            return "进行中";
        }
        return s;
    }


}
