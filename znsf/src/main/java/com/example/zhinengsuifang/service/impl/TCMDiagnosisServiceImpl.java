package com.example.zhinengsuifang.service.impl;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.TCMDiagnosis;
import com.example.zhinengsuifang.mapper.TCMDiagnosisMapper;
import com.example.zhinengsuifang.service.TCMDiagnosisService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TCMDiagnosisServiceImpl implements TCMDiagnosisService {

    @Resource
    private TCMDiagnosisMapper tcmDiagnosisMapper;

    @Override
    public Map<String, Object> createTCMDiagnosis(TCMDiagnosis diagnosis) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 如果诊断日期为null，设置为当前时间
            if (diagnosis.getDiagnosisDate() == null) {
                diagnosis.setDiagnosisDate(java.time.LocalDateTime.now());
            }
            // 如果状态为null，设置为默认值
            if (diagnosis.getStatus() == null) {
                diagnosis.setStatus("ACTIVE");
            }
            // 如果医生ID为null，设置为默认值
            if (diagnosis.getDoctorId() == null) {
                diagnosis.setDoctorId(1L); // 默认医生ID
            }
            // 如果医生姓名为null，设置为默认值
            if (diagnosis.getDoctorName() == null) {
                diagnosis.setDoctorName("系统医生"); // 默认医生姓名
            }
            int count = tcmDiagnosisMapper.insert(diagnosis);
            if (count > 0) {
                result.put("success", true);
                result.put("code", ApiCode.SUCCESS.getCode());
                result.put("message", "创建成功");
                result.put("data", diagnosis);
            } else {
                result.put("success", false);
                result.put("code", ApiCode.INTERNAL_ERROR.getCode());
                result.put("message", "创建失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> updateTCMDiagnosis(TCMDiagnosis diagnosis) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = tcmDiagnosisMapper.update(diagnosis);
            if (count > 0) {
                result.put("success", true);
                result.put("code", ApiCode.SUCCESS.getCode());
                result.put("message", "更新成功");
                result.put("data", diagnosis);
            } else {
                result.put("success", false);
                result.put("code", ApiCode.NOT_FOUND.getCode());
                result.put("message", "记录不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getTCMDiagnosisById(Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            TCMDiagnosis diagnosis = tcmDiagnosisMapper.selectById(id);
            if (diagnosis != null) {
                result.put("success", true);
                result.put("code", ApiCode.SUCCESS.getCode());
                result.put("message", "查询成功");
                result.put("data", diagnosis);
            } else {
                result.put("success", false);
                result.put("code", ApiCode.NOT_FOUND.getCode());
                result.put("message", "记录不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getLatestTCMDiagnosisByPatientId(Long patientId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TCMDiagnosis diagnosis = tcmDiagnosisMapper.selectLatestByPatientId(patientId);
            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", "查询成功");
            result.put("data", diagnosis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getTCMDiagnosisListByPatientId(Long patientId, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<TCMDiagnosis> list = tcmDiagnosisMapper.selectByPatientId(patientId, limit);
            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", "查询成功");
            result.put("data", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> deleteTCMDiagnosisById(Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = tcmDiagnosisMapper.deleteById(id);
            if (count > 0) {
                result.put("success", true);
                result.put("code", ApiCode.SUCCESS.getCode());
                result.put("message", "删除成功");
            } else {
                result.put("success", false);
                result.put("code", ApiCode.NOT_FOUND.getCode());
                result.put("message", "记录不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + e.getMessage());
        }
        return result;
    }
}