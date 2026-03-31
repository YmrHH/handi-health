package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.entity.RevisitRecord;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.RevisitRecordMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.RevisitRecordService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
/**
 * 复诊记录相关業务实现。
 * <p>
 * 由医生为患者新增复诊信息，并记录创建者医生信息。
 * </p>
 */
public class RevisitRecordServiceImpl implements RevisitRecordService {

    @Resource
    private RevisitRecordMapper revisitRecordMapper;

    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    /**
     * 医生新增患者复诊记录。
     *
     * @param record 复诊记录
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> addRevisitRecord(RevisitRecord record, String doctorUsername, String doctorPassword) {
        Map<String, Object> result = new HashMap<>();

        if (record == null || record.getPatientId() == null || record.getRevisitAt() == null
                || record.getHospital() == null || record.getDepartment() == null || record.getNotes() == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        if (record.getHospital().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "hospital 不能为空");
            return result;
        }

        if (record.getDepartment().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "department 不能为空");
            return result;
        }

        if (record.getNotes().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "notes 不能为空");
            return result;
        }

        if (record.getRevisitAt().isAfter(LocalDateTime.now())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "复诊时间不能晚于当前时间");
            return result;
        }

        if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "医生账号或密码不能为空");
            return result;
        }

        // 医生身份校驗
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

        // 校驗患者存在
        User patient = userMapper.findById(record.getPatientId());
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        if (patient.getName() == null || patient.getName().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "患者姓名不能为空");
            return result;
        }

        record.setCreatedByDoctorId(doctor.getId());
        record.setDoctorName(doctor.getName());
        record.setCreatedAt(LocalDateTime.now());

        revisitRecordMapper.insert(record);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "新增成功");
        return result;
    }
}

