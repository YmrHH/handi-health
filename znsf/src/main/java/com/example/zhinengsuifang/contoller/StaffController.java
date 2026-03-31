package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.PatientFollowupRecord;
import com.example.zhinengsuifang.entity.FollowUpTask;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.FollowUpScheduleMapper;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupRecordMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@Tag(name = "员工", description = "员工端相关接口（随访员/医生）")
public class StaffController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PatientFollowupRecordMapper patientFollowupRecordMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private FollowUpScheduleMapper followUpScheduleMapper;

    private Long currentUserId(HttpServletRequest request) {
        return AuthHeaderUtil.getUserId(request);
    }

    private Map<String, Object> requireStaff(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("code", ApiCode.UNAUTHORIZED.getCode());
            err.put("message", "未登录或登录已过期");
            return err;
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("code", ApiCode.UNAUTHORIZED.getCode());
            err.put("message", "用户不存在");
            return err;
        }
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        if (!"DOCTOR".equals(role) && !"FOLLOW_UP".equals(role)) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("code", ApiCode.FORBIDDEN.getCode());
            err.put("message", "无权限");
            return err;
        }
        return null;
    }

    private Long parseLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            String s = String.valueOf(obj).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            String s = String.valueOf(obj).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String toStr(Object obj) {
        if (obj == null) {
            return null;
        }
        String s = String.valueOf(obj).trim();
        return s.isEmpty() ? null : s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map<?, ?>) {
            return (Map<String, Object>) obj;
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                if (value instanceof String && ((String) value).trim().isEmpty()) {
                    continue;
                }
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(Object... values) {
        Object value = firstNonNull(values);
        if (value == null) {
            return null;
        }
        return toStr(value);
    }

    private String mergeUniqueText(Object... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        java.util.LinkedHashSet<String> parts = new java.util.LinkedHashSet<>();
        for (Object value : values) {
            String s = toStr(value);
            if (s != null && !s.trim().isEmpty()) {
                parts.add(s.trim());
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("；", parts);
    }

    private LocalDateTime parseDateTime(Object value) {
        String s = toStr(value);
        if (s == null) {
            return null;
        }
        String text = s.replace('/', '-');
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
        }
        try {
            return java.time.LocalDate.parse(text).atStartOfDay();
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获取员工个人资料
     */
    @GetMapping("/profile")
    public Map<String, Object> getProfile(HttpServletRequest request) {
        Map<String, Object> err = requireStaff(request);
        if (err != null) {
            return err;
        }
        Long userId = currentUserId(request);
        User user = userMapper.findById(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("phone", user.getPhone());
        data.put("age", user.getAge());
        data.put("sex", user.getSex());
        data.put("address", user.getAddress());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    /**
     * 更新员工个人资料
     */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireStaff(request);
        if (err != null) {
            return err;
        }
        Long userId = currentUserId(request);

        if (body != null) {
            String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
            String address = body.get("address") == null ? null : String.valueOf(body.get("address"));
            Integer age = null;
            if (body.get("age") instanceof Number) {
                age = ((Number) body.get("age")).intValue();
            }
            String sex = body.get("sex") == null ? null : String.valueOf(body.get("sex"));
            String phone = body.get("phone") == null ? null : String.valueOf(body.get("phone"));
            userMapper.updateProfileById(userId, name, address, age, sex, phone);
        }

        User user = userMapper.findById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("phone", user.getPhone());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "更新成功");
        result.put("data", data);
        return result;
    }

    /**
     * 提交随访记录
     * 现在这里直接闭环：
     * 1. 新增 patient_followup_record
     * 2. 若带 taskId，则同步 follow_up_task.status = COMPLETED
     * 3. 若带 taskId，则同步 follow_up_schedule.status = COMPLETED, completed_at = now()
     */
    @PostMapping("/followups")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitFollowup(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireStaff(request);
        if (err != null) {
            return err;
        }

        Long staffId = currentUserId(request);
        User staff = userMapper.findById(staffId);

        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Long patientId = parseLong(body.get("patientId"));
        Long taskId = parseLong(body.get("taskId"));
        Map<String, Object> form = asMap(body.get("form"));
        Map<String, Object> labs = asMap(body.get("labs"));
        Map<String, Object> tcm = asMap(body.get("tcm"));

        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "患者ID不能为空");
            return result;
        }

        PatientFollowupRecord record = new PatientFollowupRecord();
        record.setPatientId(patientId);
        record.setStaffId(staffId);
        if (staff != null && "DOCTOR".equalsIgnoreCase(staff.getRole())) {
            record.setDoctorId(staffId);
        }

        record.setFollowupTime(parseDateTime(firstNonNull(body.get("followupTime"), body.get("visitTime"), body.get("createdAt"))));
        if (record.getFollowupTime() == null) {
            record.setFollowupTime(LocalDateTime.now());
        }
        record.setFollowupType(firstNonBlank(body.get("followupType"), body.get("contactMode"), body.get("mode")));
        record.setResultStatus(firstNonBlank(body.get("resultStatus"), body.get("contactResult"), "COMPLETED"));
        record.setRiskLevel(firstNonBlank(body.get("riskLevel"), body.get("riskLevelText")));
        record.setSymptomChange(firstNonBlank(body.get("mainSymptoms"), form == null ? null : form.get("symptomChange"), body.get("symptomChange")));
        record.setContentSummary(firstNonBlank(body.get("summary"), form == null ? null : form.get("summary"), body.get("contentSummary")));

        record.setSbp(parseDouble(firstNonNull(body.get("sbp"), form == null ? null : form.get("sbp"), body.get("bloodPressureHigh"))));
        record.setDbp(parseDouble(firstNonNull(body.get("dbp"), form == null ? null : form.get("dbp"), body.get("bloodPressureLow"))));
        record.setHeartRate(parseDouble(firstNonNull(body.get("heartRate"), body.get("hr"), form == null ? null : form.get("heartRate"), form == null ? null : form.get("hr"))));
        record.setWeightKg(parseDouble(firstNonNull(body.get("weightKg"), body.get("weight"), form == null ? null : form.get("weightKg"), form == null ? null : form.get("weight"))));
        record.setVitalMeasureTime(parseDateTime(firstNonNull(body.get("vitalMeasureTime"), form == null ? null : form.get("vitalMeasureTime"), form == null ? null : form.get("measureTime"))));

        record.setMedAdherence(firstNonBlank(body.get("adherence"), body.get("medAdherence"), form == null ? null : form.get("medAdherence")));
        record.setMedPlanSummary(firstNonBlank(body.get("medicineName"), body.get("medPlanSummary"), form == null ? null : form.get("medicineName"), form == null ? null : form.get("medPlanSummary")));
        record.setAdverseReaction(firstNonBlank(body.get("adverseReaction"), form == null ? null : form.get("adverseReaction")));

        record.setTcmFace(firstNonBlank(body.get("tcmFace"), tcm == null ? null : tcm.get("face"), tcm == null ? null : tcm.get("faceIndex")));
        record.setTcmTongueBody(firstNonBlank(body.get("tcmTongueBody"), tcm == null ? null : tcm.get("tongueBody"), tcm == null ? null : tcm.get("tongueBodyIndex")));
        record.setTcmTongueCoating(firstNonBlank(body.get("tcmTongueCoating"), tcm == null ? null : tcm.get("tongueCoating"), tcm == null ? null : tcm.get("tongueCoatIndex")));
        record.setTcmTongueImageUrl(firstNonBlank(body.get("tcmTongueImageUrl"), tcm == null ? null : tcm.get("tongueUrl"), tcm == null ? null : tcm.get("tongueImage")));
        record.setTcmPulseRate(firstNonBlank(body.get("tcmPulseRate"), tcm == null ? null : tcm.get("pulseRate")));
        record.setTcmPulseTypes(firstNonBlank(body.get("tcmPulseTypes"), tcm == null ? null : tcm.get("pulseDesc")));
        String legacyTcmConclusion = firstNonBlank(body.get("tcmConclusion"), tcm == null ? null : tcm.get("summary"), tcm == null ? null : tcm.get("summaryIndex"));
        String legacyTcmRemark = firstNonBlank(body.get("tcmRemark"), tcm == null ? null : tcm.get("comment"));
        String tcmEvaluationAdvice = firstNonBlank(body.get("tcmEvaluationAdvice"), body.get("tcmAdvice"), tcm == null ? null : tcm.get("evaluationAdvice"), tcm == null ? null : tcm.get("advice"), tcm == null ? null : tcm.get("comment"));
        String finalTcmEvaluationAdvice = firstNonBlank(tcmEvaluationAdvice, mergeUniqueText(legacyTcmConclusion, legacyTcmRemark));
        record.setTcmConclusion(firstNonBlank(finalTcmEvaluationAdvice, legacyTcmConclusion));
        record.setTcmRemark(firstNonBlank(finalTcmEvaluationAdvice, legacyTcmRemark));

        String healthAdvice = firstNonBlank(body.get("healthAdvice"), body.get("labSummary"), labs == null ? null : labs.get("healthAdvice"), labs == null ? null : labs.get("comment"));
        record.setLabSummary(healthAdvice);
        record.setNextFollowupType(firstNonBlank(body.get("nextFollowupType"), body.get("nextFollowupPlan"), body.get("nextPlan")));
        record.setNextFollowupDate(parseDateTime(firstNonNull(body.get("nextFollowupDate"), body.get("nextVisitDate"), body.get("nextPlanTime"))));
        record.setNextFollowupRemark(firstNonBlank(body.get("nextFollowupRemark"), body.get("nextPlanRemark"), body.get("nextRemark")));

        record.setExt1(firstNonBlank(body.get("advice"), form == null ? null : form.get("advice"), healthAdvice, finalTcmEvaluationAdvice));
        record.setExt2(firstNonBlank(body.get("medicineName"), record.getMedPlanSummary()));
        record.setExt3(firstNonBlank(body.get("contactResult"), body.get("mode")));
        record.setExt4(taskId == null ? null : String.valueOf(taskId));
        record.setExt5(firstNonBlank(body.get("alertId")));

        patientFollowupRecordMapper.insert(record);

        // 同步更新患者主档案风险等级（user.risk_level），确保任务页/患者页风险口径一致
        String riskForProfile = firstNonBlank(record.getRiskLevel(), body.get("riskLevel"), body.get("riskLevelText"));
        if (riskForProfile != null) {
            String s = riskForProfile.trim().toUpperCase();
            String normalizedRisk = null;
            if (s.contains("HIGH") || s.contains("高")) {
                normalizedRisk = "HIGH";
            } else if (s.contains("LOW") || s.contains("低")) {
                normalizedRisk = "LOW";
            } else if (!s.isEmpty()) {
                normalizedRisk = "MID";
            }
            if (normalizedRisk != null) {
                userMapper.updateRiskLevelById(patientId, normalizedRisk);
            }
        }

        // 若前端未传 taskId，则按 patientId 兜底查一条“待处理任务”，避免随访任务状态长期停留在未完成
        if (taskId == null) {
            FollowUpTask activeTask = followUpTaskMapper.findLatestActiveByPatientId(patientId);
            if (activeTask != null) {
                taskId = activeTask.getId();
            }
        }

        if (taskId != null) {
            followUpTaskMapper.updateStatus(taskId, "COMPLETED");
            followUpScheduleMapper.updateByTaskId(taskId, "COMPLETED", LocalDateTime.now());
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "提交成功");

        Map<String, Object> data = new HashMap<>();
        data.put("recordId", record.getId());
        data.put("taskId", taskId);
        result.put("data", data);

        return result;
    }

    /**
     * 根据随访任务ID获取最近一次随访记录（小程序医护端使用）
     *
     * 前端调用：GET /staff/followups/by-task/{taskId}
     */
    @GetMapping("/followups/by-task/{taskId}")
    public Map<String, Object> getFollowupByTask(HttpServletRequest request,
                                                 @PathVariable("taskId") Long taskId) {
        Map<String, Object> err = requireStaff(request);
        if (err != null) {
            return err;
        }

        Map<String, Object> result = new HashMap<>();
        if (taskId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "任务ID不能为空");
            return result;
        }

        FollowUpTask task = followUpTaskMapper.findById(taskId);
        if (task == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "随访任务不存在");
            return result;
        }

        PatientFollowupRecord record = patientFollowupRecordMapper.findLatestByTaskId(String.valueOf(taskId));

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("patientId", task.getPatientId());
        data.put("record", record);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", record == null ? "暂无随访记录" : "查询成功");
        result.put("data", data);
        return result;
    }

    /**
     * 更新任务状态
     * 这里也补齐 schedule 同步，避免前端单独调用时首页统计不更新。
     */
    @PutMapping("/tasks/{taskId}/status")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTaskStatus(HttpServletRequest request,
                                                @PathVariable("taskId") Long taskId,
                                                @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireStaff(request);
        if (err != null) {
            return err;
        }

        Map<String, Object> result = new HashMap<>();
        if (taskId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "任务ID不能为空");
            return result;
        }

        String status = body == null ? null : (body.get("status") == null ? null : String.valueOf(body.get("status")));
        if (status == null || status.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "状态不能为空");
            return result;
        }

        String normalizedStatus = status.trim().toUpperCase();
        int updated = followUpTaskMapper.updateStatus(taskId, normalizedStatus);

        if (updated > 0 && "COMPLETED".equals(normalizedStatus)) {
            followUpScheduleMapper.updateByTaskId(taskId, "COMPLETED", LocalDateTime.now());
        }

        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.NOT_FOUND.getCode());
        result.put("message", updated > 0 ? "更新成功" : "任务不存在");
        return result;
    }
}