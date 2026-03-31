package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.dto.CandidatePatient;
import com.example.zhinengsuifang.entity.RevisitRecord;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.HealthMetricMapper;
import com.example.zhinengsuifang.mapper.RevisitRecordMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.CandidateService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 候選患者相关業务实现。
 * <p>
 * 根据規則篩選需要重點随访的患者，例如：
 * 1) 近期多次血壓異常（非極端值）
 * 2) 長期未复诊
 * </p>
 */
public class CandidateServiceImpl implements CandidateService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private HealthMetricMapper healthMetricMapper;

    @Resource
    private RevisitRecordMapper revisitRecordMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    /**
     * 医生端查询候選患者列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码（明文）
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> candidatePatients(String doctorUsername, String doctorPassword) {
        Map<String, Object> result = new HashMap<>();

        if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "医生账号或密码不能为空");
            return result;
        }

        // 仅允许医生查询
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

        // 查询所有患者 id，逐一判斷是否命中候選規則
        List<Long> patientIds = userMapper.findPatientIds();
        List<CandidatePatient> candidates = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        // 近期異常判斷窗口與閾值
        int abnormalDaysWindow = 14;
        int abnormalCountThreshold = 5;

        // 長期未复诊判斷天数
        int noRevisitDays = 90;

        if (patientIds != null) {
            for (Long patientId : patientIds) {
                if (patientId == null) {
                    continue;
                }

                LocalDateTime startAt = now.minusDays(abnormalDaysWindow);
                Long abnormalCount = healthMetricMapper.countBpAbnormalNonExtreme(patientId, startAt, now);
                if (abnormalCount != null && abnormalCount >= abnormalCountThreshold) {
                    CandidatePatient c = new CandidatePatient();
                    c.setPatientId(patientId);
                    c.setReason("LONG_ABNORMAL");
                    c.setAbnormalCount(abnormalCount);
                    c.setDays((long) abnormalDaysWindow);
                    candidates.add(c);
                    continue;
                }

                RevisitRecord latest = revisitRecordMapper.findLatestByPatientId(patientId);
                if (latest == null || latest.getRevisitAt() == null) {
                    CandidatePatient c = new CandidatePatient();
                    c.setPatientId(patientId);
                    c.setReason("NO_REVISIT");
                    c.setAbnormalCount(0L);
                    c.setDays((long) noRevisitDays);
                    candidates.add(c);
                    continue;
                }

                long days = ChronoUnit.DAYS.between(latest.getRevisitAt(), now);
                if (days >= noRevisitDays) {
                    CandidatePatient c = new CandidatePatient();
                    c.setPatientId(patientId);
                    c.setReason("NO_REVISIT");
                    c.setAbnormalCount(0L);
                    c.setDays(days);
                    candidates.add(c);
                }
            }
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", candidates);
        return result;
    }
}

