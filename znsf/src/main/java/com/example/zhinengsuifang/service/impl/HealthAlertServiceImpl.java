package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.dto.CreateFollowUpFromAlertRequest;
import com.example.zhinengsuifang.dto.HealthAlertWithPatient;
import com.example.zhinengsuifang.dto.MarkHealthAlertRequest;
import com.example.zhinengsuifang.entity.FollowUpTask;
import com.example.zhinengsuifang.entity.FollowUpSchedule;
import com.example.zhinengsuifang.entity.HealthAlert;
import com.example.zhinengsuifang.entity.HealthMetric;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.FollowUpScheduleMapper;
import com.example.zhinengsuifang.mapper.HealthAlertMapper;
import com.example.zhinengsuifang.mapper.HealthMetricMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.HealthAlertService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 健康预警相关業务实现。
 * <p>
 * 主要包含：
 * 1) 医生查询预警列表
 * 2) 基於预警生成随访单
 * 3) 医生标记预警状态
 * </p>
 */
public class HealthAlertServiceImpl implements HealthAlertService {

    @Resource
    private HealthAlertMapper healthAlertMapper;

    @Resource
    private HealthMetricMapper healthMetricMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private FollowUpScheduleMapper followUpScheduleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User requireDoctorById(Long doctorId) {
        if (doctorId == null) {
            return null;
        }
        User doctor = userMapper.findById(doctorId);
        if (doctor == null || doctor.getRole() == null) {
            return null;
        }
        if (!"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            return null;
        }
        return doctor;
    }

    @Override
    public Map<String, Object> getAlertForm(String doctorUsername, String doctorPassword, Long alertId) {
        Map<String, Object> result = new HashMap<>();

        if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty() || alertId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        User doctor = userMapper.findByUsername(doctorUsername.trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        boolean doctorMatch = passwordEncoder.matches(doctorPassword, doctor.getPassword());
        if (!doctorMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return result;
        }

        HealthAlert alert = healthAlertMapper.findById(alertId);
        if (alert == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "预警不存在");
            return result;
        }

        if (!canDoctorAccessPatient(doctor.getId(), alert.getPatientId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限查看該患者");
            return result;
        }

        User patient = userMapper.findPatientById(alert.getPatientId());

        Map<String, Object> patientInfo = new HashMap<>();
        patientInfo.put("patientId", alert.getPatientId());
        if (patient != null) {
            patientInfo.put("name", patient.getName());
            patientInfo.put("phone", patient.getPhone());
            patientInfo.put("riskLevel", patient.getRiskLevel());
        }

        Map<String, Object> event = new HashMap<>();
        event.put("alertId", alert.getId());
        event.put("severity", alert.getSeverity());
        event.put("status", alert.getStatus());
        event.put("createdAt", alert.getCreatedAt());
        event.put("triggerMetricType", alert.getMetricType());

        List<Map<String, Object>> formSchema = new ArrayList<>();
        formSchema.add(buildFieldSchema("HR", "心率", "bpm", 1));
        formSchema.add(buildFieldSchema("PULSE", "脉搏", "bpm", 2));
        formSchema.add(buildFieldSchema("SPO2", "血氧", "%", 3));

        Map<String, Object> formData = new HashMap<>();
        Map<String, Object> current = new HashMap<>();
        Map<String, Object> normal = new HashMap<>();

        HealthMetric latestHr = healthMetricMapper.findLatestByPatientIdAndType(alert.getPatientId(), "HR");
        HealthMetric latestPulse = healthMetricMapper.findLatestByPatientIdAndType(alert.getPatientId(), "PULSE");
        HealthMetric latestSpo2 = healthMetricMapper.findLatestByPatientIdAndType(alert.getPatientId(), "SPO2");

        current.put("HR", latestHr == null ? null : latestHr.getValue1());
        current.put("PULSE", latestPulse == null ? null : latestPulse.getValue1());
        current.put("SPO2", latestSpo2 == null ? null : latestSpo2.getValue1());
        current.put("measuredAt", newestMeasuredAt(latestHr, latestPulse, latestSpo2));

        normal.put("HR", null);
        normal.put("PULSE", null);
        normal.put("SPO2", null);

        formData.put("current", current);
        formData.put("normal", normal);

        List<Map<String, Object>> abnormalItems = buildAbnormalItems(alert, current);

        Map<String, Object> data = new HashMap<>();
        data.put("patient", patientInfo);
        data.put("event", event);
        data.put("formSchema", formSchema);
        data.put("formData", formData);
        data.put("abnormalItems", abnormalItems);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getAlertFormByDoctorId(Long doctorId, Long alertId) {
        Map<String, Object> result = new HashMap<>();
        if (doctorId == null || alertId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        User doctor = requireDoctorById(doctorId);
        if (doctor == null) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        HealthAlert alert = healthAlertMapper.findById(alertId);
        if (alert == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "预警不存在");
            return result;
        }

        if (!canDoctorAccessPatient(doctor.getId(), alert.getPatientId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限查看該患者");
            return result;
        }

        // 复用原 form 组装逻辑：将 doctorUsername/doctorPassword 的校验替换为已验证 doctorId
        User patient = userMapper.findPatientById(alert.getPatientId());

        Map<String, Object> patientInfo = new HashMap<>();
        patientInfo.put("patientId", alert.getPatientId());
        if (patient != null) {
            patientInfo.put("name", patient.getName());
            patientInfo.put("phone", patient.getPhone());
            patientInfo.put("riskLevel", patient.getRiskLevel());
        }

        Map<String, Object> event = new HashMap<>();
        event.put("alertId", alert.getId());
        event.put("severity", alert.getSeverity());
        event.put("status", alert.getStatus());
        event.put("createdAt", alert.getCreatedAt());
        event.put("triggerMetricType", alert.getMetricType());

        List<Map<String, Object>> formSchema = new ArrayList<>();
        formSchema.add(buildFieldSchema("HR", "心率", "bpm", 1));
        formSchema.add(buildFieldSchema("PULSE", "脉搏", "bpm", 2));
        formSchema.add(buildFieldSchema("SPO2", "血氧", "%", 3));

        Map<String, Object> formData = new HashMap<>();
        Map<String, Object> current = new HashMap<>();
        Map<String, Object> normal = new HashMap<>();

        HealthMetric latestHr = healthMetricMapper.findLatestByPatientIdAndType(alert.getPatientId(), "HR");
        HealthMetric latestPulse = healthMetricMapper.findLatestByPatientIdAndType(alert.getPatientId(), "PULSE");
        HealthMetric latestSpo2 = healthMetricMapper.findLatestByPatientIdAndType(alert.getPatientId(), "SPO2");

        current.put("HR", latestHr == null ? null : latestHr.getValue1());
        current.put("PULSE", latestPulse == null ? null : latestPulse.getValue1());
        current.put("SPO2", latestSpo2 == null ? null : latestSpo2.getValue1());
        current.put("measuredAt", newestMeasuredAt(latestHr, latestPulse, latestSpo2));

        normal.put("HR", null);
        normal.put("PULSE", null);
        normal.put("SPO2", null);

        formData.put("current", current);
        formData.put("normal", normal);

        List<Map<String, Object>> abnormalItems = buildAbnormalItems(alert, current);

        Map<String, Object> data = new HashMap<>();
        data.put("patient", patientInfo);
        data.put("event", event);
        data.put("formSchema", formSchema);
        data.put("formData", formData);
        data.put("abnormalItems", abnormalItems);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @Override
    /**
     * 医生查询预警列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param status 可選状态过濾
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> listAlerts(String doctorUsername, String doctorPassword, String status) {
        Map<String, Object> result = new HashMap<>();

        if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "医生账号或密码不能为空");
            return result;
        }

        // 医生身份校驗（仅 DOCTOR 可查询）
        User doctor = userMapper.findByUsername(doctorUsername.trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        boolean doctorMatch = passwordEncoder.matches(doctorPassword, doctor.getPassword());
        if (!doctorMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return result;
        }

        List<HealthAlert> alerts;
        if (status == null || status.trim().isEmpty()) {
            alerts = healthAlertMapper.findAll();
        } else {
            alerts = healthAlertMapper.findByStatus(status.trim().toUpperCase());
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", alerts);
        return result;
    }

    @Override
    public Map<String, Object> listAlertsByDoctorId(Long doctorId, String status) {
        Map<String, Object> result = new HashMap<>();

        User doctor = requireDoctorById(doctorId);
        if (doctor == null) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        List<HealthAlert> alerts;
        if (status == null || status.trim().isEmpty()) {
            alerts = healthAlertMapper.findAll();
        } else {
            alerts = healthAlertMapper.findByStatus(status.trim().toUpperCase());
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", alerts);
        return result;
    }

    @Override
    public Map<String, Object> searchAlertsByPatientName(String doctorUsername,
                                                         String doctorPassword,
                                                         String patientName,
                                                         String status,
                                                         Integer limit) {
        Map<String, Object> result = new HashMap<>();

        if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "医生账号或密码不能为空");
            return result;
        }

        User doctor = userMapper.findByUsername(doctorUsername.trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        boolean doctorMatch = passwordEncoder.matches(doctorPassword, doctor.getPassword());
        if (!doctorMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return result;
        }

        int lim = 50;
        if (limit != null && limit > 0) {
            lim = Math.min(limit, 200);
        }

        String st = null;
        if (status != null && !status.trim().isEmpty()) {
            st = status.trim().toUpperCase();
        }

        List<HealthAlertWithPatient> list = healthAlertMapper.findAlertsByPatientName(
                patientName == null ? null : patientName.trim(),
                st,
                lim
        );

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @Override
    public Map<String, Object> searchAlertsByPatientNameByDoctorId(Long doctorId,
                                                                    String patientName,
                                                                    String status,
                                                                    Integer limit) {
        Map<String, Object> result = new HashMap<>();

        User doctor = requireDoctorById(doctorId);
        if (doctor == null) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        int lim = 50;
        if (limit != null && limit > 0) {
            lim = Math.min(limit, 200);
        }

        String st = null;
        if (status != null && !status.trim().isEmpty()) {
            st = status.trim().toUpperCase();
        }

        List<HealthAlertWithPatient> list = healthAlertMapper.findAlertsByPatientName(
                patientName == null ? null : patientName.trim(),
                st,
                lim
        );

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @Override
    /**
     * 由预警生成随访单。
     *
     * @param request 生成请求
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> createFollowUpFromAlert(CreateFollowUpFromAlertRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null || request.getAlertId() == null || request.getFollowUpUserId() == null
                || request.getDoctorUsername() == null || request.getDoctorPassword() == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        User doctor = userMapper.findByUsername(request.getDoctorUsername().trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        boolean doctorMatch = passwordEncoder.matches(request.getDoctorPassword(), doctor.getPassword());
        if (!doctorMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return result;
        }

        HealthAlert alert = healthAlertMapper.findById(request.getAlertId());
        if (alert == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "预警不存在");
            return result;
        }

        // 已处理的预警不允许重複生成随访单
        if (alert.getStatus() != null && !"NEW".equalsIgnoreCase(alert.getStatus().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.CONFLICT.getCode());
            result.put("message", "预警已处理");
            return result;
        }

        User followUpUser = userMapper.findById(request.getFollowUpUserId());
        if (followUpUser == null || followUpUser.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(followUpUser.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "followUpUserId 不存在或不是随访员");
            return result;
        }

        FollowUpTask task = new FollowUpTask();
        task.setPatientId(alert.getPatientId());
        task.setDoctorId(doctor.getId());
        task.setFollowUpUserId(request.getFollowUpUserId());
        task.setTriggerType("ALERT");
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            task.setDescription(request.getDescription());
        } else {
            task.setDescription("健康数据急剧变化预警");
        }
        task.setStatus("ASSIGNED");

        followUpTaskMapper.insert(task);

        FollowUpSchedule schedule = new FollowUpSchedule();
        schedule.setPatientId(task.getPatientId());
        schedule.setPlanId(null);
        if (request.getDueAt() != null) {
            schedule.setDueAt(request.getDueAt());
        } else {
            schedule.setDueAt(LocalDateTime.now());
        }
        schedule.setStatus("DUE");
        schedule.setCompletedAt(null);
        schedule.setRelatedTaskId(task.getId());
        followUpScheduleMapper.insert(schedule);

        healthAlertMapper.updateStatus(alert.getId(), "FOLLOWUP_CREATED");

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "生成随访单成功");
        return result;
    }

    @Override
    public Map<String, Object> createFollowUpFromAlertByDoctorId(Long doctorId,
                                                                  Long alertId,
                                                                  Long followUpUserId,
                                                                  String description,
                                                                  LocalDateTime dueAt) {
        Map<String, Object> result = new HashMap<>();

        User doctor = requireDoctorById(doctorId);
        if (doctor == null) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        if (alertId == null || followUpUserId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        HealthAlert alert = healthAlertMapper.findById(alertId);
        if (alert == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "预警不存在");
            return result;
        }

        if (!canDoctorAccessPatient(doctor.getId(), alert.getPatientId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限查看該患者");
            return result;
        }

        // 已处理的预警不允许重複生成随访单
        if (alert.getStatus() != null && !"NEW".equalsIgnoreCase(alert.getStatus().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.CONFLICT.getCode());
            result.put("message", "预警已处理");
            return result;
        }

        User followUpUser = userMapper.findById(followUpUserId);
        if (followUpUser == null || followUpUser.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(followUpUser.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "followUpUserId 不存在或不是随访员");
            return result;
        }

        FollowUpTask task = new FollowUpTask();
        task.setPatientId(alert.getPatientId());
        task.setDoctorId(doctor.getId());
        task.setFollowUpUserId(followUpUserId);
        task.setTriggerType("ALERT");
        if (description != null && !description.trim().isEmpty()) {
            task.setDescription(description);
        } else {
            task.setDescription("健康数据急剧变化预警");
        }
        task.setStatus("ASSIGNED");

        followUpTaskMapper.insert(task);

        FollowUpSchedule schedule = new FollowUpSchedule();
        schedule.setPatientId(task.getPatientId());
        schedule.setPlanId(null);
        schedule.setDueAt(dueAt != null ? dueAt : LocalDateTime.now());
        schedule.setStatus("DUE");
        schedule.setCompletedAt(null);
        schedule.setRelatedTaskId(task.getId());
        followUpScheduleMapper.insert(schedule);

        healthAlertMapper.updateStatus(alert.getId(), "FOLLOWUP_CREATED");

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "生成随访单成功");
        return result;
    }

    @Override
    /**
     * 医生标记预警状态。
     *
     * @param request 标记请求
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> markAlert(MarkHealthAlertRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null || request.getAlertId() == null || request.getStatus() == null
                || request.getDoctorUsername() == null || request.getDoctorPassword() == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        User doctor = userMapper.findByUsername(request.getDoctorUsername().trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        boolean doctorMatch = passwordEncoder.matches(request.getDoctorPassword(), doctor.getPassword());
        if (!doctorMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return result;
        }

        HealthAlert alert = healthAlertMapper.findById(request.getAlertId());
        if (alert == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "预警不存在");
            return result;
        }

        String newStatus = request.getStatus().trim().toUpperCase();
        if (!"REVIEWED".equals(newStatus) && !"IGNORED".equals(newStatus)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "status 仅允许 REVIEWED 或 IGNORED");
            return result;
        }

        if (alert.getStatus() != null && !"NEW".equalsIgnoreCase(alert.getStatus().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.CONFLICT.getCode());
            result.put("message", "预警已处理");
            return result;
        }

        int updated = healthAlertMapper.updateStatus(alert.getId(), newStatus);
        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", updated > 0 ? "更新成功" : "更新失败");
        return result;
    }

    @Override
    public Map<String, Object> markAlertByDoctorId(Long doctorId, Long alertId, String status) {
        Map<String, Object> result = new HashMap<>();

        User doctor = requireDoctorById(doctorId);
        if (doctor == null) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        if (alertId == null || status == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        HealthAlert alert = healthAlertMapper.findById(alertId);
        if (alert == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "预警不存在");
            return result;
        }

        if (!canDoctorAccessPatient(doctor.getId(), alert.getPatientId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限查看該患者");
            return result;
        }

        String newStatus = status.trim().toUpperCase();
        if (!"REVIEWED".equals(newStatus) && !"IGNORED".equals(newStatus)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "status 仅允许 REVIEWED 或 IGNORED");
            return result;
        }

        if (alert.getStatus() != null && !"NEW".equalsIgnoreCase(alert.getStatus().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.CONFLICT.getCode());
            result.put("message", "预警已处理");
            return result;
        }

        int updated = healthAlertMapper.updateStatus(alert.getId(), newStatus);
        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", updated > 0 ? "更新成功" : "更新失败");
        return result;
    }

    private boolean canDoctorAccessPatient(Long doctorId, Long patientId) {
        if (doctorId == null || patientId == null) {
            return false;
        }
        Long cnt = followUpTaskMapper.countByDoctorIdAndPatientId(doctorId, patientId);
        return cnt != null && cnt > 0;
    }

    private Map<String, Object> buildFieldSchema(String key, String label, String unit, int order) {
        Map<String, Object> field = new HashMap<>();
        field.put("key", key);
        field.put("label", label);
        field.put("unit", unit);
        field.put("order", order);
        field.put("group", "vitals");
        return field;
    }

    private LocalDateTime newestMeasuredAt(HealthMetric a, HealthMetric b, HealthMetric c) {
        LocalDateTime t = null;
        if (a != null && a.getMeasuredAt() != null) {
            t = a.getMeasuredAt();
        }
        if (b != null && b.getMeasuredAt() != null && (t == null || b.getMeasuredAt().isAfter(t))) {
            t = b.getMeasuredAt();
        }
        if (c != null && c.getMeasuredAt() != null && (t == null || c.getMeasuredAt().isAfter(t))) {
            t = c.getMeasuredAt();
        }
        return t;
    }

    private List<Map<String, Object>> buildAbnormalItems(HealthAlert alert, Map<String, Object> current) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (alert == null || alert.getMetricType() == null) {
            return items;
        }
        String type = alert.getMetricType().trim().toUpperCase();
        Object currObj = current.get(type);
        Double currValue = currObj instanceof Number ? ((Number) currObj).doubleValue() : null;

        Double prev = alert.getPrevValue1();
        Double curr = alert.getCurrValue1();
        if (currValue == null && curr != null) {
            currValue = curr;
        }

        if (currValue == null) {
            return items;
        }

        if (isSingleAbnormal(type, currValue)) {
            Map<String, Object> item = new HashMap<>();
            item.put("metricType", type);
            item.put("ruleType", "SINGLE");
            item.put("prevValue", prev);
            item.put("currValue", currValue);
            item.put("severity", alert.getSeverity());
            items.add(item);
        }

        if (prev != null && (curr != null || currValue != null)) {
            Double usedCurr = curr != null ? curr : currValue;
            if (isTrendAbnormal(type, prev, usedCurr)) {
                Map<String, Object> item = new HashMap<>();
                item.put("metricType", type);
                item.put("ruleType", "TREND");
                item.put("prevValue", prev);
                item.put("currValue", usedCurr);
                item.put("delta", usedCurr - prev);
                item.put("severity", alert.getSeverity());
                items.add(item);
            }
        }

        return items;
    }

    private boolean isSingleAbnormal(String metricType, Double currValue) {
        if (metricType == null || currValue == null) {
            return false;
        }
        metricType = metricType.trim().toUpperCase();
        if ("HR".equals(metricType) || "PULSE".equals(metricType)) {
            return currValue < 50 || currValue > 100;
        }
        if ("SPO2".equals(metricType)) {
            return currValue < 92;
        }
        if ("GLUCOSE".equals(metricType) || "GLUCOSE_FASTING".equals(metricType) || "GLUCOSE_POSTPRANDIAL".equals(metricType)) {
            // 空腹血糖：低于2.8 或 ≥7.0
            if ("GLUCOSE_FASTING".equals(metricType)) {
                return currValue < 2.8 || currValue >= 7.0;
            }
            // 餐后2小时血糖：≥11.1
            else if ("GLUCOSE_POSTPRANDIAL".equals(metricType)) {
                return currValue >= 11.1;
            }
            // 随机血糖：低于3.9 或 ≥11.1
            else {
                return currValue < 3.9 || currValue >= 11.1;
            }
        }
        return false;
    }

    private boolean isTrendAbnormal(String metricType, Double prevValue, Double currValue) {
        if (metricType == null || prevValue == null || currValue == null) {
            return false;
        }
        metricType = metricType.trim().toUpperCase();
        double deltaAbs = Math.abs(currValue - prevValue);
        if ("HR".equals(metricType) || "PULSE".equals(metricType)) {
            return deltaAbs >= 30;
        }
        if ("SPO2".equals(metricType)) {
            return deltaAbs >= 5;
        }
        if ("GLUCOSE".equals(metricType) || "GLUCOSE_FASTING".equals(metricType) || "GLUCOSE_POSTPRANDIAL".equals(metricType)) {
            // 血糖趋势异常：短期内变化超过 3.0 mmol/L
            return deltaAbs >= 3.0;
        }
        return false;
    }
}

