package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.dto.PatientBrief;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 用户相关業务实现。
 */
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    /**
     * 新增用户。
     *
     * @param user 用户信息
     */
    public void addUser(User user) {
        userMapper.insert(user);
    }

    @Override
    /**
     * 统计患者总数。
     *
     * @return 患者数量
     */
    public Long countPatients() {
        return userMapper.countPatients();
    }

    @Override
    /**
     * 患者风险等级统计：返返各等级数量與占比。
     *
     * @return 统计结果（total/counts/ratios）
     */
    public Map<String, Object> patientRiskStats() {
        Long total = userMapper.countPatients();
        if (total == null) {
            total = 0L;
        }

        // 初始化三個等级，避免前端缺 key
        Map<String, Long> counts = new HashMap<>();
        counts.put("低", 0L);
        counts.put("中", 0L);
        counts.put("高", 0L);

        List<com.example.zhinengsuifang.dto.RiskLevelCount> dbCounts = userMapper.countPatientsGroupByRiskLevel();
        if (dbCounts != null) {
            for (com.example.zhinengsuifang.dto.RiskLevelCount row : dbCounts) {
                if (row == null) {
                    continue;
                }
                String level = row.getRiskLevel();
                Long c = row.getCount();
                if (level == null || level.trim().isEmpty()) {
                    continue;
                }
                level = level.trim();
                if (c == null) {
                    c = 0L;
                }
                if (counts.containsKey(level)) {
                    counts.put(level, c);
                }
            }
        }

        Map<String, BigDecimal> ratios = new HashMap<>();
        ratios.put("低", ratio(counts.get("低"), total));
        ratios.put("中", ratio(counts.get("中"), total));
        ratios.put("高", ratio(counts.get("高"), total));

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("counts", counts);
        data.put("ratios", ratios);
        return data;
    }

    @Override
    public User findPatientById(Long id) {
        return userMapper.findPatientById(id);
    }

    @Override
    public List<PatientBrief> searchPatients(String keyword, Integer limit) {
        Integer lim = limit;
        if (lim == null || lim <= 0) {
            lim = 50;
        }
        if (lim > 200) {
            lim = 200;
        }
        return userMapper.searchPatients(keyword, lim);
    }

    @Override
    public Map<String, Object> getPatientByIdForDoctor(String doctorUsername, String doctorPassword, Long patientId) {
        Map<String, Object> result = new HashMap<>();

        if (doctorUsername == null || doctorUsername.trim().isEmpty()
                || doctorPassword == null || doctorPassword.trim().isEmpty()) {
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

        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "患者ID不能为空");
            return result;
        }

        User patient = userMapper.findPatientById(patientId);
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", patient);
        return result;
    }

    @Override
    public Map<String, Object> searchPatientsForDoctor(String doctorUsername, String doctorPassword, String keyword, Integer limit) {
        Map<String, Object> result = new HashMap<>();

        if (doctorUsername == null || doctorUsername.trim().isEmpty()
                || doctorPassword == null || doctorPassword.trim().isEmpty()) {
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

        List<PatientBrief> list = searchPatients(keyword, limit);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    private BigDecimal ratio(Long count, Long total) {
        if (count == null) {
            count = 0L;
        }
        if (total == null || total == 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}



