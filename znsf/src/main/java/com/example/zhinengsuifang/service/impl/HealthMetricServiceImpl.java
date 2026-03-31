package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.entity.HealthMetric;
import com.example.zhinengsuifang.entity.HealthAlert;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.HealthAlertMapper;
import com.example.zhinengsuifang.mapper.HealthMetricMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.HealthMetricService;
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
 * 健康指标相关業务实现。
 * <p>
 * 支持患者上報指标、医生按患者維度查询指标，并在血壓等指标出现「短期突变」時生成健康预警。
 * </p>
 */
public class HealthMetricServiceImpl implements HealthMetricService {

    @Resource
    private HealthMetricMapper healthMetricMapper;

    @Resource
    private HealthAlertMapper healthAlertMapper;

    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    /**
     * 新增健康指标数据。
     * <p>
     * 寫入后如果存在上一條同類型指标，且本次变化幅度達到閾值，則生成预警。
     * </p>
     *
     * @param metric 指标数据
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> addMetric(HealthMetric metric) {
        Map<String, Object> result = new HashMap<>();

        if (metric == null || metric.getPatientId() == null || metric.getMetricType() == null || metric.getMetricType().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        // 统一指标类型：避免小程序上报使用小写/带空格导致无法找到 prev，从而不生成预警
        metric.setMetricType(metric.getMetricType().trim().toUpperCase());

        if (metric.getMeasuredAt() == null) {
            metric.setMeasuredAt(LocalDateTime.now());
        }

        // 用於判斷是否需要生成「突变预警」
        HealthMetric prev = null;
        if (metric.getMetricType() != null && !metric.getMetricType().trim().isEmpty()) {
            prev = healthMetricMapper.findLatestByPatientIdAndType(metric.getPatientId(), metric.getMetricType().trim());
        }

        healthMetricMapper.insert(metric);

        // 检查单一值异常（如低血糖），即使没有之前的记录
        if (isSingleAbnormal(metric.getMetricType(), metric.getValue1())) {
            HealthAlert alert = new HealthAlert();
            alert.setPatientId(metric.getPatientId());
            alert.setMetricType(metric.getMetricType());
            alert.setPrevValue1(null);
            alert.setPrevValue2(null);
            alert.setCurrValue1(metric.getValue1());
            alert.setCurrValue2(metric.getValue2());
            alert.setDeltaValue1(null);
            alert.setDeltaValue2(null);
            alert.setSeverity("WARN");
            alert.setStatus("NEW");
            healthAlertMapper.insert(alert);
        } else if (prev != null) {
            createAlertIfSuddenChange(prev, metric);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "新增成功");
        return result;
    }

    @Override
    /**
     * 医生查询患者最新指标列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientId 患者 ID
     * @param limit 條数上限（可選）
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> doctorLatestMetrics(String doctorUsername, String doctorPassword, Long patientId, Integer limit) {
        Map<String, Object> result = new HashMap<>();

        // 统一的医生身份校驗
        User doctor = authDoctor(result, doctorUsername, doctorPassword);
        if (doctor == null) {
            return result;
        }

        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "patientId 不能为空");
            return result;
        }

        User patient = userMapper.findById(patientId);
        if (patient == null || patient.getRole() == null || !"PATIENT".equalsIgnoreCase(patient.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        int n = 50;
        if (limit != null && limit > 0) {
            n = Math.min(limit, 200);
        }

        List<HealthMetric> list = healthMetricMapper.findLatestByPatientIdLimit(patientId, n);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @Override
    /**
     * 医生按條件查询患者指标。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientId 患者 ID
     * @param metricType 指标類型（可選）
     * @param startAt 起始時間
     * @param endAt 结束時間
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> doctorQueryMetrics(String doctorUsername,
                                                  String doctorPassword,
                                                  Long patientId,
                                                  String metricType,
                                                  LocalDateTime startAt,
                                                  LocalDateTime endAt) {
        Map<String, Object> result = new HashMap<>();

        User doctor = authDoctor(result, doctorUsername, doctorPassword);
        if (doctor == null) {
            return result;
        }

        if (patientId == null || startAt == null || endAt == null) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "patientId/startAt/endAt 不能为空");
            return result;
        }

        if (endAt.isBefore(startAt)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "endAt 不能早於 startAt");
            return result;
        }

        User patient = userMapper.findById(patientId);
        if (patient == null || patient.getRole() == null || !"PATIENT".equalsIgnoreCase(patient.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        String type = metricType;
        if (type != null && type.trim().isEmpty()) {
            type = null;
        }
        if (type != null) {
            type = type.trim();
        }

        List<HealthMetric> list = healthMetricMapper.findByPatientIdAndTypeAndRange(patientId, type, startAt, endAt);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    private User authDoctor(Map<String, Object> result, String doctorUsername, String doctorPassword) {
        if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "医生账号或密码不能为空");
            return null;
        }

        User doctor = userMapper.findByUsername(doctorUsername.trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return null;
        }

        boolean match = passwordEncoder.matches(doctorPassword, doctor.getPassword());
        if (!match) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return null;
        }

        return doctor;
    }

    private void createAlertIfSuddenChange(HealthMetric prev, HealthMetric curr) {
        String metricType = curr.getMetricType();
        if (metricType == null) {
            return;
        }
        metricType = metricType.trim().toUpperCase();

        if (!"BP".equals(metricType) && !"HR".equals(metricType) && !"PULSE".equals(metricType) && !"SPO2".equals(metricType) && !"GLUCOSE".equals(metricType) && !"GLUCOSE_FASTING".equals(metricType) && !"GLUCOSE_POSTPRANDIAL".equals(metricType)) {
            return;
        }

        if ("BP".equals(metricType)) {
            Double prevSbp = prev.getValue1();
            Double prevDbp = prev.getValue2();
            Double currSbp = curr.getValue1();
            Double currDbp = curr.getValue2();
            if (prevSbp == null || prevDbp == null || currSbp == null || currDbp == null) {
                return;
            }

            // 低血压：收缩压 < 90 或 舒张压 < 60
            boolean lowBp = currSbp < 90 || currDbp < 60;
            if (lowBp) {
                HealthAlert alert = new HealthAlert();
                alert.setPatientId(curr.getPatientId());
                alert.setMetricType(metricType);
                alert.setPrevValue1(prevSbp);
                alert.setPrevValue2(prevDbp);
                alert.setCurrValue1(currSbp);
                alert.setCurrValue2(currDbp);
                alert.setDeltaValue1(null);
                alert.setDeltaValue2(null);
                alert.setSeverity("WARN");
                alert.setStatus("NEW");
                healthAlertMapper.insert(alert);
                return;
            }

            // 高血压检测
            boolean isHypertension = false;
            String severity = "WARN";
            
            // 三级高血压（重度）：收缩压≥180 或 舒张压≥110
            if (currSbp >= 180 || currDbp >= 110) {
                isHypertension = true;
                severity = "HIGH";
            }
            // 二级高血压（中度）：收缩压160～179 或 舒张压100～109
            else if ((currSbp >= 160 && currSbp <= 179) || (currDbp >= 100 && currDbp <= 109)) {
                isHypertension = true;
                severity = "HIGH";
            }
            // 一级高血压（轻度）：收缩压140～159 或 舒张压90～99
            else if ((currSbp >= 140 && currSbp <= 159) || (currDbp >= 90 && currDbp <= 99)) {
                isHypertension = true;
            }
            // 单纯收缩期高血压：收缩压≥140 且 舒张压＜90
            else if (currSbp >= 140 && currDbp < 90) {
                isHypertension = true;
            }

            if (isHypertension) {
                HealthAlert alert = new HealthAlert();
                alert.setPatientId(curr.getPatientId());
                alert.setMetricType(metricType);
                alert.setPrevValue1(prevSbp);
                alert.setPrevValue2(prevDbp);
                alert.setCurrValue1(currSbp);
                alert.setCurrValue2(currDbp);
                alert.setDeltaValue1(null);
                alert.setDeltaValue2(null);
                alert.setSeverity(severity);
                alert.setStatus("NEW");
                healthAlertMapper.insert(alert);
                return;
            }

            // 血压突变：收缩压变化 ≥ 30 或 舒张压变化 ≥ 20
            double deltaSbp = Math.abs(currSbp - prevSbp);
            double deltaDbp = Math.abs(currDbp - prevDbp);

            boolean sudden = deltaSbp >= 30 || deltaDbp >= 20;
            if (!sudden) {
                return;
            }

            HealthAlert alert = new HealthAlert();
            alert.setPatientId(curr.getPatientId());
            alert.setMetricType(metricType);
            alert.setPrevValue1(prevSbp);
            alert.setPrevValue2(prevDbp);
            alert.setCurrValue1(currSbp);
            alert.setCurrValue2(currDbp);
            alert.setDeltaValue1(deltaSbp);
            alert.setDeltaValue2(deltaDbp);
            alert.setSeverity("WARN");
            alert.setStatus("NEW");
            healthAlertMapper.insert(alert);
            return;
        }

        Double prevValue = prev.getValue1();
        Double currValue = curr.getValue1();
        if (prevValue == null || currValue == null) {
            return;
        }

        List<String> matchedRules = new ArrayList<>();
        if (isSingleAbnormal(metricType, currValue)) {
            matchedRules.add("SINGLE");
        }
        if (isTrendAbnormal(metricType, prevValue, currValue)) {
            matchedRules.add("TREND");
        }
        if (matchedRules.isEmpty()) {
            return;
        }

        double delta = currValue - prevValue;
        HealthAlert alert = new HealthAlert();
        alert.setPatientId(curr.getPatientId());
        alert.setMetricType(metricType);
        alert.setPrevValue1(prevValue);
        alert.setPrevValue2(null);
        alert.setCurrValue1(currValue);
        alert.setCurrValue2(null);
        alert.setDeltaValue1(delta);
        alert.setDeltaValue2(null);
        alert.setSeverity("WARN");
        alert.setStatus("NEW");
        healthAlertMapper.insert(alert);
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

