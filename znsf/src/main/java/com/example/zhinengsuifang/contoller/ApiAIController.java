package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.TCMDiagnosis;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.TCMDiagnosisMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "AI相关接口")
public class ApiAIController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TCMDiagnosisMapper tcmDiagnosisMapper;

    @GetMapping("/risk-assessment/{patientId}")
    public Map<String, Object> riskAssessment(@PathVariable("patientId") Long patientId) {
        Map<String, Object> result = new HashMap<>();
        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: patientId");
            return result;
        }

        User patient = userMapper.findPatientById(patientId);
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        // 获取中医四诊数据
        TCMDiagnosis latestTCM = tcmDiagnosisMapper.selectLatestByPatientId(patientId);

        // 计算AI风险评估
        Map<String, Object> assessment = new HashMap<>();
        assessment.put("aiScore", calculateAIScore(patient, latestTCM));
        assessment.put("aiFocus", generateAIFocus(patient, latestTCM));
        assessment.put("aiAdvice", generateAIAdvice(patient, latestTCM));

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "评估成功");
        result.put("data", assessment);
        return result;
    }

    // 计算AI风险评分
    private String calculateAIScore(User patient, TCMDiagnosis tcm) {
        // 模拟AI评分逻辑
        int score = 0;
        
        // 基于年龄
        if (patient.getAge() != null) {
            if (patient.getAge() > 60) score += 20;
            else if (patient.getAge() > 40) score += 10;
        }
        
        // 基于风险等级
        if (patient.getRiskLevel() != null) {
            String risk = patient.getRiskLevel().toUpperCase();
            if (risk.contains("HIGH") || risk.contains("高")) score += 30;
            else if (risk.contains("MID") || risk.contains("中")) score += 15;
        }
        
        // 基于中医四诊
        if (tcm != null) {
            if (tcm.getTcmSummary() != null && tcm.getTcmSummary().contains("虚")) score += 10;
            if (tcm.getPulseDescription() != null && tcm.getPulseDescription().contains("弱")) score += 10;
        }
        
        // 确保分数在0-100之间
        score = Math.min(100, Math.max(0, score));
        return String.valueOf(score);
    }

    // 生成AI重点关注
    private String generateAIFocus(User patient, TCMDiagnosis tcm) {
        StringBuilder focus = new StringBuilder();
        
        if (patient.getAge() != null && patient.getAge() > 60) {
            focus.append("年龄较大，");
        }
        
        if (patient.getRiskLevel() != null) {
            String risk = patient.getRiskLevel().toUpperCase();
            if (risk.contains("HIGH") || risk.contains("高")) {
                focus.append("风险等级高，");
            }
        }
        
        if (tcm != null) {
            if (tcm.getTcmSummary() != null) {
                if (tcm.getTcmSummary().contains("虚")) focus.append("体质虚弱，");
                if (tcm.getTcmSummary().contains("湿")) focus.append("湿气较重，");
            }
            if (tcm.getPulseDescription() != null) {
                if (tcm.getPulseDescription().contains("弱")) focus.append("脉象虚弱，");
                if (tcm.getPulseDescription().contains("快")) focus.append("心率偏快，");
            }
        }
        
        if (focus.length() > 0) {
            focus.setLength(focus.length() - 1);
            return focus.toString();
        } else {
            return "暂无特殊关注";
        }
    }

    // 生成AI建议
    private String generateAIAdvice(User patient, TCMDiagnosis tcm) {
        StringBuilder advice = new StringBuilder();
        
        advice.append("1. 保持良好的生活习惯，规律作息，避免熬夜。\n");
        advice.append("2. 饮食宜清淡，避免辛辣刺激性食物。\n");
        advice.append("3. 适当进行有氧运动，如散步、太极拳等。\n");
        
        if (tcm != null) {
            if (tcm.getTcmSummary() != null) {
                if (tcm.getTcmSummary().contains("虚")) {
                    advice.append("4. 可适当食用补气养血的食物，如红枣、桂圆等。\n");
                }
                if (tcm.getTcmSummary().contains("湿")) {
                    advice.append("4. 可食用祛湿的食物，如薏米、红豆等。\n");
                }
            }
        }
        
        advice.append("5. 定期监测健康指标，如有不适及时就医。");
        return advice.toString();
    }
}
