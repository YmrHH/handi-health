package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.dto.CreateFollowUpTaskRequest;
import com.example.zhinengsuifang.dto.CancelFollowUpTaskRequest;
import com.example.zhinengsuifang.dto.RepublishFollowUpTaskRequest;
import com.example.zhinengsuifang.entity.FollowUpTask;
import com.example.zhinengsuifang.entity.FollowUpSchedule;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.FollowUpScheduleMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.FollowUpTaskService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 随访任务（派单）相关業务实现。
 * <p>
 * 主要包含：
 * 1) 医生创建派单
 * 2) 医生/随访员查询任务列表
 * 3) 医生取消任务、随访员更新任务状态
 * </p>
 */
public class FollowUpTaskServiceImpl implements FollowUpTaskService {

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private FollowUpScheduleMapper followUpScheduleMapper;

    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    /**
     * 医生派单：创建随访任务。
     *
     * @param request 派单请求
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> createTask(CreateFollowUpTaskRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null || request.getDoctorUsername() == null || request.getDoctorPassword() == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        // 医生身份校驗（仅 DOCTOR 可派单）
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

        if (request.getPatientId() == null || request.getFollowUpUserId() == null) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "patientId 或 followUpUserId 不能为空");
            return result;
        }

        // 校驗 patient/followUpUser 是否存在且角色正確
        User patient = userMapper.findById(request.getPatientId());
        User followUpUser = userMapper.findById(request.getFollowUpUserId());

        if (patient == null || patient.getRole() == null || !"PATIENT".equalsIgnoreCase(patient.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "patientId 不存在或不是患者");
            return result;
        }

        if (followUpUser == null || followUpUser.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(followUpUser.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "followUpUserId 不存在或不是随访员");
            return result;
        }

        FollowUpTask task = new FollowUpTask();
        task.setPatientId(request.getPatientId());
        task.setDoctorId(doctor.getId());
        task.setFollowUpUserId(request.getFollowUpUserId());

        String triggerType = request.getTriggerType();
        if (triggerType == null || triggerType.trim().isEmpty()) {
            triggerType = "DOCTOR_MANUAL";
        }
        task.setTriggerType(triggerType.trim());
        task.setDescription(request.getDescription());
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

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "派单成功");
        return result;
    }

    @Override
    /**
     * 医生查询自己的派单列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> listTasksByDoctor(String doctorUsername, String doctorPassword) {
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

        List<FollowUpTask> tasks = followUpTaskMapper.findByDoctorId(doctor.getId());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", tasks);
        return result;
    }

    @Override
    /**
     * 随访员查询自己的任务列表。
     *
     * @param followUpUsername 随访员账号
     * @param followUpPassword 随访员密码
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> listMyTasks(String followUpUsername, String followUpPassword) {
        Map<String, Object> result = new HashMap<>();

        if (followUpUsername == null || followUpUsername.trim().isEmpty() || followUpPassword == null || followUpPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "随访员账号或密码不能为空");
            return result;
        }

        User followUpUser = userMapper.findByUsername(followUpUsername.trim());
        if (followUpUser == null || followUpUser.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(followUpUser.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        boolean match = passwordEncoder.matches(followUpPassword, followUpUser.getPassword());
        if (!match) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "随访员账号或密码错誤");
            return result;
        }

        List<FollowUpTask> tasks = followUpTaskMapper.findByFollowUpUserId(followUpUser.getId());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", tasks);
        return result;
    }

    @Override
    public Map<String, Object> republishTask(RepublishFollowUpTaskRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null || request.getTaskId() == null
                || request.getDoctorUsername() == null || request.getDoctorUsername().trim().isEmpty()
                || request.getDoctorPassword() == null || request.getDoctorPassword().trim().isEmpty()
                || request.getFollowUpUserId() == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        FollowUpTask oldTask = followUpTaskMapper.findById(request.getTaskId());
        if (oldTask == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "任务不存在");
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

        if (!doctor.getId().equals(oldTask.getDoctorId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "只能修改自己派出的任务");
            return result;
        }

        User followUpUser = userMapper.findById(request.getFollowUpUserId());
        if (followUpUser == null || followUpUser.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(followUpUser.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "followUpUserId 不存在或不是随访员");
            return result;
        }

        // 1) 取消原任务 + 同步取消原计划
        followUpTaskMapper.updateStatus(oldTask.getId(), "CANCELED");
        followUpScheduleMapper.updateByTaskId(oldTask.getId(), "CANCELED", null);

        // 2) 新建任务（等同重新发布）
        FollowUpTask newTask = new FollowUpTask();
        newTask.setPatientId(oldTask.getPatientId());
        newTask.setDoctorId(doctor.getId());
        newTask.setFollowUpUserId(request.getFollowUpUserId());

        String triggerType = request.getTriggerType();
        if (triggerType == null || triggerType.trim().isEmpty()) {
            triggerType = oldTask.getTriggerType();
        }
        if (triggerType == null || triggerType.trim().isEmpty()) {
            triggerType = "DOCTOR_MANUAL";
        }
        newTask.setTriggerType(triggerType.trim());
        newTask.setDescription(request.getDescription());
        newTask.setStatus("ASSIGNED");
        followUpTaskMapper.insert(newTask);

        FollowUpSchedule schedule = new FollowUpSchedule();
        schedule.setPatientId(newTask.getPatientId());
        schedule.setPlanId(null);
        schedule.setDueAt(request.getDueAt() == null ? LocalDateTime.now() : request.getDueAt());
        schedule.setStatus("DUE");
        schedule.setCompletedAt(null);
        schedule.setRelatedTaskId(newTask.getId());
        followUpScheduleMapper.insert(schedule);

        Map<String, Object> data = new HashMap<>();
        data.put("oldTaskId", oldTask.getId());
        data.put("newTaskId", newTask.getId());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "重新发布成功");
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> cancelTask(CancelFollowUpTaskRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null || request.getTaskId() == null
                || request.getDoctorUsername() == null || request.getDoctorUsername().trim().isEmpty()
                || request.getDoctorPassword() == null || request.getDoctorPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        FollowUpTask task = followUpTaskMapper.findById(request.getTaskId());
        if (task == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "任务不存在");
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

        if (!doctor.getId().equals(task.getDoctorId())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "只能取消自己派出的任务");
            return result;
        }

        int updated = followUpTaskMapper.updateStatus(task.getId(), "CANCELED");
        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", updated > 0 ? "取消成功" : "取消失败");
        return result;
    }
}

