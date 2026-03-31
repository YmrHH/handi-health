package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.PatientMessage;
import com.example.zhinengsuifang.entity.PatientFollowupRecord;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.PatientBasicInfoMapper;
import com.example.zhinengsuifang.mapper.PatientDailyMeasurementMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupRecordMapper;
import com.example.zhinengsuifang.mapper.PatientLabResultMapper;
import com.example.zhinengsuifang.mapper.PatientMessageMapper;
import com.example.zhinengsuifang.mapper.PatientRehabTaskMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.util.ApiResponseUtil;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "医护端", description = "小程序医护端接口（患者数据查看与建议下发）")
public class ApiStaffController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PatientBasicInfoMapper patientBasicInfoMapper;

    @Resource
    private PatientDailyMeasurementMapper patientDailyMeasurementMapper;

    @Resource
    private PatientLabResultMapper patientLabResultMapper;

    @Resource
    private PatientRehabTaskMapper patientRehabTaskMapper;

    @Resource
    private PatientMessageMapper patientMessageMapper;

    @Resource
    private PatientFollowupRecordMapper patientFollowupRecordMapper;

    private User requireStaff(HttpServletRequest request) {
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

    /**
     * 小程序端数据刷新：根据最新随访记录刷新“最近随访建议”等视图数据
     * 用途：Web 端编辑/提交随访记录成功后，显式调用本接口，让小程序端再次拉取时能看到最新内容。
     */
    @PostMapping("/patients/{patientId}/sync-followup")
    public Map<String, Object> syncFollowupForMiniProgram(HttpServletRequest request,
                                                          @PathVariable("patientId") Long patientId,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        User staff = requireStaff(request);
        if (staff == null) {
            return ApiResponseUtil.fail(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (patientId == null) {
            return ApiResponseUtil.fail(ApiCode.PARAM_ERROR, "patientId 不能为空");
        }

        User patient = userMapper.findById(patientId);
        if (patient == null) {
            return ApiResponseUtil.fail(ApiCode.NOT_FOUND, "患者不存在");
        }

        Long recordId = null;
        if (body != null && body.get("recordId") != null) {
            try {
                recordId = Long.parseLong(String.valueOf(body.get("recordId")));
            } catch (Exception ignored) {
                recordId = null;
            }
        }

        PatientFollowupRecord latestRecord = null;
        if (recordId != null) {
            // mapper 限定了 patientId + id 双键，避免跨患者误读
            latestRecord = patientFollowupRecordMapper.findById(patientId, recordId);
        }

        if (latestRecord == null) {
            // 未指定 recordId 或找不到对应记录时，按 patientId 取最近一条
            List<Map<String, Object>> recent = patientFollowupRecordMapper.findRecentByPatientId(patientId, 1);
            if (recent != null && !recent.isEmpty()) {
                Object idVal = recent.get(0).get("id");
                if (idVal != null) {
                    try {
                        Long id = Long.parseLong(String.valueOf(idVal));
                        latestRecord = patientFollowupRecordMapper.findById(patientId, id);
                    } catch (Exception ignored) {
                        latestRecord = null;
                    }
                }
            }
        }

        if (latestRecord == null) {
            return ApiResponseUtil.fail(ApiCode.NOT_FOUND, "未找到随访记录");
        }

        // 按当前小程序视图约定：
        // 1) patientData() 中优先通过 PatientMessage.latestAdvice 展示“最近随访建议”
        // 2) 若没有 PatientMessage，再从随访记录 ext1 等字段兜底
        // 这里直接写入一条新的 PatientMessage，让 latestAdvice 与最新随访保持同步。
        String adviceContent = latestRecord.getExt1();
        if (adviceContent == null || adviceContent.trim().isEmpty()) {
            adviceContent = latestRecord.getLabSummary();
        }
        if (adviceContent == null) {
            adviceContent = "";
        }

        PatientMessage message = new PatientMessage();
        message.setPatientId(patientId);
        message.setTitle("最近随访建议");
        message.setContent(adviceContent.trim());
        message.setMessageType("ADVICE");
        message.setStatus("NEW");
        message.setReadAt(null);
        message.setExt1(String.valueOf(staff.getId()));
        message.setExt2(staff.getName());
        message.setExt3(staff.getRole());
        message.setExt4(null);
        message.setExt5(null);
        patientMessageMapper.insert(message);

        Map<String, Object> data = new HashMap<>();
        data.put("patientId", patientId);
        data.put("recordId", latestRecord.getId());
        data.put("messageId", message.getId());

        return ApiResponseUtil.ok(data);
    }

    @GetMapping("/patients/{patientId}/data")
    public Map<String, Object> patientData(HttpServletRequest request,
                                           @PathVariable("patientId") Long patientId,
                                           @RequestParam(required = false) Integer historyLimit) {
        User staff = requireStaff(request);
        if (staff == null) {
            return ApiResponseUtil.fail(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (patientId == null) {
            return ApiResponseUtil.fail(ApiCode.PARAM_ERROR, "patientId 不能为空");
        }

        User patient = userMapper.findById(patientId);
        if (patient == null) {
            return ApiResponseUtil.fail(ApiCode.NOT_FOUND, "患者不存在");
        }

        int limit = historyLimit == null ? 30 : Math.max(1, Math.min(historyLimit, 200));

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        var basicInfo = patientBasicInfoMapper.findByPatientId(patientId);
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", patient.getId());
        profile.put("name", patient.getName());
        profile.put("age", patient.getAge());
        profile.put("gender", patient.getSex());
        profile.put("phone", patient.getPhone());
        profile.put("address", patient.getAddress());
        profile.put("riskLevel", patient.getRiskLevel());
        profile.put("riskText", riskText(patient.getRiskLevel()));
        profile.put("basicInfo", basicInfo);
        // 兼容小程序 staff/patientData 页面字段：profile.disease/profile.mainDisease
        // mainDisease / syndrome 存在 patient_basic_info.ext3/ext4
        profile.put("mainDisease", basicInfo == null ? null : basicInfo.getExt3());
        profile.put("disease", basicInfo == null ? null : basicInfo.getExt3());
        profile.put("syndrome", basicInfo == null ? null : basicInfo.getExt4());

        Map<String, Object> today = new HashMap<>();
        today.put("rows", patientDailyMeasurementMapper.findInRange(patientId, start, end));
        today.put("latest", patientDailyMeasurementMapper.findLatest(patientId));

        List<?> history = patientDailyMeasurementMapper.findRecent(patientId, limit);
        List<?> labs = patientLabResultMapper.findByPatientId(patientId);
        List<?> rehabTasks = patientRehabTaskMapper.findByPatientId(patientId);
        List<?> messages = patientMessageMapper.findByPatientId(patientId);
        List<Map<String, Object>> followups = patientFollowupRecordMapper.findPatientListByPatientId(patientId);
        Map<String, Object> followupLatest = (followups == null || followups.isEmpty()) ? null : new HashMap<>(followups.get(0));
        if (followupLatest != null) {
            if (isBlank(followupLatest.get("advice")) && !isBlank(followupLatest.get("ext1"))) {
                followupLatest.put("advice", followupLatest.get("ext1"));
            }
            if (isBlank(followupLatest.get("medicineName")) && !isBlank(followupLatest.get("medPlanSummary"))) {
                followupLatest.put("medicineName", followupLatest.get("medPlanSummary"));
            }
        }

        PatientMessage latestMessage = patientMessageMapper.findLatestByPatientId(patientId);
        Map<String, Object> latestAdvice = buildAdvice(latestMessage);
        if (latestAdvice == null && followupLatest != null && !isBlank(followupLatest.get("ext1"))) {
            latestAdvice = new HashMap<>();
            latestAdvice.put("title", "最近随访建议");
            latestAdvice.put("doctor", firstNonBlank(followupLatest.get("followupStaffName"), followupLatest.get("doctorName"), followupLatest.get("staffName"), "责任医生"));
            latestAdvice.put("time", formatDateTime(followupLatest.get("followupTime")));
            latestAdvice.put("content", String.valueOf(followupLatest.get("ext1")));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);
        data.put("today", today);
        data.put("history", history);
        data.put("labs", labs);
        data.put("rehabTasks", rehabTasks);
        data.put("messages", messages);
        data.put("followups", followups);
        data.put("followupLatest", followupLatest);
        data.put("latestAdvice", latestAdvice);

        return ApiResponseUtil.ok(data);
    }

    @PostMapping("/patients/{patientId}/advice")
    public Map<String, Object> sendAdvice(HttpServletRequest request,
                                          @PathVariable("patientId") Long patientId,
                                          @RequestBody Map<String, Object> body) {
        User staff = requireStaff(request);
        if (staff == null) {
            return ApiResponseUtil.fail(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (patientId == null) {
            return ApiResponseUtil.fail(ApiCode.PARAM_ERROR, "patientId 不能为空");
        }

        User patient = userMapper.findById(patientId);
        if (patient == null) {
            return ApiResponseUtil.fail(ApiCode.NOT_FOUND, "患者不存在");
        }

        String content = body == null ? null : (body.get("content") == null ? null : String.valueOf(body.get("content")));
        if (content == null || content.trim().isEmpty()) {
            return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "content 不能为空");
        }

        String title = body == null ? null : (body.get("title") == null ? null : String.valueOf(body.get("title")));
        if (title == null || title.trim().isEmpty()) {
            title = "医护建议";
        }

        PatientMessage m = new PatientMessage();
        m.setPatientId(patientId);
        m.setTitle(title);
        m.setContent(content.trim());
        m.setMessageType("ADVICE");
        m.setStatus("NEW");
        m.setReadAt(null);
        m.setExt1(String.valueOf(staff.getId()));
        m.setExt2(staff.getName());
        m.setExt3(staff.getRole());
        m.setExt4(null);
        m.setExt5(null);

        patientMessageMapper.insert(m);

        Map<String, Object> data = new HashMap<>();
        data.put("messageId", m.getId());
        return ApiResponseUtil.ok(data);
    }

    private static String riskText(String level) {
        if (level == null) {
            return "风险未知";
        }
        String s = level.trim().toUpperCase();
        if (s.contains("HIGH") || s.contains("高")) {
            return "高风险";
        }
        if (s.contains("LOW") || s.contains("低")) {
            return "低风险";
        }
        return "中风险";
    }

    private static Map<String, Object> buildAdvice(PatientMessage message) {
        if (message == null || isBlank(message.getContent())) {
            return null;
        }
        Map<String, Object> advice = new HashMap<>();
        advice.put("title", isBlank(message.getTitle()) ? "医生健康建议" : message.getTitle());
        advice.put("doctor", firstNonBlank(message.getExt2(), "责任医生"));
        advice.put("time", formatDateTime(message.getCreatedAt()));
        advice.put("content", message.getContent());
        return advice;
    }

    private static String formatDateTime(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).replace('T', ' ');
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
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
}
