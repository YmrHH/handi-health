package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.PatientBasicInfo;
import com.example.zhinengsuifang.entity.PatientDailyMeasurement;
import com.example.zhinengsuifang.entity.PatientFeedback;
import com.example.zhinengsuifang.entity.PatientFollowupApplication;
import com.example.zhinengsuifang.entity.PatientFollowupRecord;
import com.example.zhinengsuifang.entity.PatientLabResult;
import com.example.zhinengsuifang.entity.PatientLinkedAccount;
import com.example.zhinengsuifang.entity.PatientMessage;
import com.example.zhinengsuifang.entity.PatientRehabTask;
import com.example.zhinengsuifang.entity.PatientTcmSurvey;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.entity.HealthMetric;
import com.example.zhinengsuifang.mapper.PatientBasicInfoMapper;
import com.example.zhinengsuifang.mapper.PatientDailyMeasurementMapper;
import com.example.zhinengsuifang.mapper.PatientFeedbackMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupApplicationMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupRecordMapper;
import com.example.zhinengsuifang.mapper.PatientLabResultMapper;
import com.example.zhinengsuifang.mapper.PatientLinkedAccountMapper;
import com.example.zhinengsuifang.mapper.PatientMessageMapper;
import com.example.zhinengsuifang.mapper.PatientRehabTaskMapper;
import com.example.zhinengsuifang.mapper.PatientTcmSurveyMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.HealthMetricService;
import com.example.zhinengsuifang.util.ApiResponseUtil;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import com.example.zhinengsuifang.util.ValidationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/patient")
@Tag(name = "患者", description = "患者端接口（含硬件/日测量数据上报与查询）")
public class PatientController {

    @Schema(name = "PatientDailyMeasurementRequest", description = "患者日测量上报请求体。时间字段 measuredAt 支持 'yyyy-MM-dd HH:mm:ss' 或 ISO-8601（如 '2026-01-26T11:40:00'）。")
    public static class PatientDailyMeasurementRequest {
        @Schema(description = "测量时间", example = "2026-01-26 11:40:00")
        public String measuredAt;

        @Schema(description = "收缩压（mmHg）", example = "128")
        public Double sbp;

        @Schema(description = "舒张压（mmHg）", example = "82")
        public Double dbp;

        @Schema(description = "心率（次/分钟），等价字段：hr", example = "76")
        public Double heartRate;

        @Schema(description = "心率（次/分钟），heartRate 的别名", example = "76")
        public Double hr;

        @Schema(description = "体重（kg），等价字段：weight", example = "68.5")
        public Double weightKg;

        @Schema(description = "体重（kg），weightKg 的别名", example = "68.5")
        public Double weight;

        @Schema(description = "体温（℃），等价字段：temp", example = "36.6")
        public Double temperatureC;

        @Schema(description = "体温（℃），temperatureC 的别名", example = "36.6")
        public Double temp;

        @Schema(description = "血氧（%）", example = "98")
        public Double spo2;

        @Schema(description = "症状/备注", example = "无明显不适")
        public String symptoms;
        @Schema(description = "血糖（mmol/L）", example = "6.8")
        public Double glucose;

        @Schema(description = "血糖测量类型（FASTING/POSTPRANDIAL/RANDOM）", example = "FASTING")
        public String glucoseType;

        @Schema(description = "睡眠时长（小时）", example = "7.2")
        public Double sleep;
    }

    @Resource
    private UserMapper userMapper;

    @Resource
    private PatientBasicInfoMapper patientBasicInfoMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private PatientDailyMeasurementMapper patientDailyMeasurementMapper;

    @Resource
    private HealthMetricService healthMetricService;

    @Resource
    private PatientLabResultMapper patientLabResultMapper;

    @Resource
    private PatientRehabTaskMapper patientRehabTaskMapper;

    @Resource
    private PatientTcmSurveyMapper patientTcmSurveyMapper;

    @Resource
    private PatientLinkedAccountMapper patientLinkedAccountMapper;

    @Resource
    private PatientMessageMapper patientMessageMapper;

    @Resource
    private PatientFeedbackMapper patientFeedbackMapper;

    @Resource
    private PatientFollowupRecordMapper patientFollowupRecordMapper;

    @Resource
    private PatientFollowupApplicationMapper patientFollowupApplicationMapper;

    private Long currentUserId(HttpServletRequest request) {
        return AuthHeaderUtil.getUserId(request);
    }

    private Map<String, Object> requireLogin(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return ApiResponseUtil.fail(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return null;
    }

    @GetMapping("/home")
    public Map<String, Object> home(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        User patient = userMapper.findById(patientId);

        PatientBasicInfo basicInfo = patientBasicInfoMapper.findByPatientId(patientId);
        PatientDailyMeasurement latestMeasurement = patientDailyMeasurementMapper.findLatest(patientId);
        Integer unreadMessages = patientMessageMapper.countUnreadByPatientId(patientId);
        List<PatientMessage> allMessages = patientMessageMapper.findByPatientId(patientId);
        PatientMessage latestMessage = firstDoctorMessage(allMessages);
        if (latestMessage == null) {
            latestMessage = patientMessageMapper.findLatestByPatientId(patientId);
        }
        List<Map<String, Object>> followups = patientFollowupRecordMapper.findPatientListByPatientId(patientId);
        Map<String, Object> latestFollowup = (followups == null || followups.isEmpty()) ? null : new HashMap<>(followups.get(0));

        Map<String, Object> risk = new HashMap<>();
        String riskLevel = patient == null ? null : patient.getRiskLevel();
        risk.put("level", normalizeRiskLevel(riskLevel));
        risk.put("text", riskText(riskLevel));

        Map<String, Object> advice = null;
        if (latestMessage != null && toStr(latestMessage.getContent()) != null) {
            advice = new HashMap<>();
            advice.put("title", firstNonBlank(latestMessage.getTitle(), "医生健康建议"));
            advice.put("doctor", firstNonBlank(latestMessage.getExt2(), "责任医生"));
            advice.put("time", latestMessage.getCreatedAt() == null ? "" : String.valueOf(latestMessage.getCreatedAt()).replace('T', ' '));
            advice.put("content", latestMessage.getContent());
        } else if (latestFollowup != null && toStr(latestFollowup.get("ext1")) != null) {
            advice = new HashMap<>();
            advice.put("title", "最近随访建议");
            advice.put("doctor", firstNonBlank(latestFollowup.get("followupStaffName"), latestFollowup.get("doctorName"), latestFollowup.get("staffName"), "责任医生"));
            advice.put("time", firstNonBlank(latestFollowup.get("followupTime"), latestFollowup.get("createdAt"), ""));
            advice.put("content", toStr(latestFollowup.get("ext1")));
        }

        Map<String, Object> followup = new HashMap<>();
        int followupCount = followups == null ? 0 : followups.size();
        followup.put("done", followupCount);
        followup.put("total", followupCount == 0 ? 0 : followupCount);
        if (latestFollowup != null) {
            followup.put("last", firstNonBlank(latestFollowup.get("followupTime"), latestFollowup.get("createdAt")));
            followup.put("next", firstNonBlank(latestFollowup.get("nextFollowupDate"), latestFollowup.get("nextPlanTime")));
            followup.put("focus", firstNonBlank(latestFollowup.get("symptomChange"), latestFollowup.get("contentSummary"), "血压、血糖"));
            followup.put("advice", firstNonBlank(latestFollowup.get("ext1"), latestFollowup.get("contentSummary")));
        }

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        boolean dailyDone = safeLong(patientDailyMeasurementMapper.countTodayByPatientId(patientId, dayStart, dayEnd)) > 0;
        boolean surveyDone = safeLong(patientTcmSurveyMapper.countTodayByPatientId(patientId, dayStart, dayEnd)) > 0;

        Map<String, Object> data = new HashMap<>();
        data.put("basicInfo", basicInfo);
        data.put("latestMeasurement", latestMeasurement);
        data.put("unreadMessages", unreadMessages == null ? 0 : unreadMessages);
        data.put("patientName", patient == null ? null : patient.getName());
        data.put("risk", risk);
        data.put("riskLevel", risk.get("level"));
        data.put("riskText", risk.get("text"));
        data.put("advice", advice);
        data.put("followup", followup);
        data.put("dailyDone", dailyDone);
        data.put("surveyDone", surveyDone);
        data.put("hasTodaySurvey", surveyDone);
        data.put("dailyDoneDate", dailyDone ? String.valueOf(today) : "");
        data.put("surveyDoneDate", surveyDone ? String.valueOf(today) : "");
        if (latestFollowup != null) {
            data.put("nextFollowupTime", firstNonBlank(latestFollowup.get("nextFollowupDate"), latestFollowup.get("nextPlanTime")));
            data.put("lastFollowup", latestFollowup);
        }
        return ApiResponseUtil.ok(data);
    }

    @GetMapping("/basic-info")
    public Map<String, Object> getBasicInfo(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        PatientBasicInfo info = patientBasicInfoMapper.findByPatientId(patientId);
        return ApiResponseUtil.ok(info);
    }

    @PutMapping("/basic-info")
    public Map<String, Object> updateBasicInfo(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);

        PatientBasicInfo exist = patientBasicInfoMapper.findByPatientId(patientId);
        PatientBasicInfo info = exist == null ? new PatientBasicInfo() : exist;
        info.setPatientId(patientId);

        if (body != null) {
            info.setHeightCm(toDouble(body.get("heightCm"), body.get("height")));
            // 兼容多种体重字段：baselineWeightKg / baselineWeight / baseWeight（小程序）
            Object bw1 = body.get("baselineWeightKg");
            Object bw2 = body.get("baselineWeight");
            Object bw3 = body.get("baseWeight");
            info.setBaselineWeightKg(
                    toDouble(bw1 != null ? bw1 : (bw2 != null ? bw2 : bw3), null)
            );
            if (body.containsKey("idCard")) {
                String idCard = toStr(body.get("idCard"));
                if (idCard == null || idCard.trim().isEmpty()) {
                    return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "身份证号不能为空");
                }
                String normalized = ValidationUtil.normalizeIdCard(idCard);
                if (!ValidationUtil.isValidChineseIdCard18(normalized)) {
                    return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "身份证号格式不合法");
                }
                info.setIdCard(normalized);
            } else if (exist == null) {
                return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "身份证号不能为空");
            }
            info.setEmergencyContactName(toStr(body.get("emergencyContactName")));
            info.setEmergencyContactPhone(toStr(body.get("emergencyContactPhone")));
            info.setInsuranceType(toStr(body.get("insuranceType")));
            String birthday = toStr(body.get("birthday"));
            if (birthday != null && !birthday.isEmpty()) {
                try {
                    info.setBirthday(LocalDate.parse(birthday));
                } catch (Exception ignored) {
                }
            }
        }

        if (exist == null) {
            patientBasicInfoMapper.insert(info);
        } else {
            patientBasicInfoMapper.updateByPatientId(info);
        }

        return ApiResponseUtil.ok(patientBasicInfoMapper.findByPatientId(patientId));
    }

    @GetMapping("/daily-measurements/today")
    public Map<String, Object> dailyMeasurementsToday(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<PatientDailyMeasurement> rows = patientDailyMeasurementMapper.findInRange(patientId, start, end);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        return ApiResponseUtil.ok(data);
    }

    @GetMapping("/daily-measurements")
    public Map<String, Object> getDailyMeasurements(@RequestParam Long patientId, @RequestParam(defaultValue = "7") Integer days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        List<PatientDailyMeasurement> rows = patientDailyMeasurementMapper.findInRange(patientId, start, end);
        return ApiResponseUtil.ok(rows);
    }

    @PostMapping("/daily-measurements")
    @Operation(
            summary = "上报患者日测量（硬件数据）",
            description = "写入 patient_daily_measurement 表。\n\n鉴权：需要在请求头携带 Authorization: Bearer <userId>（当前登录患者的 userId）。\n\n字段说明：heartRate 与 hr 等价；weightKg 与 weight 等价；temperatureC 与 temp 等价。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功（data 为写入后的测量记录）", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "未登录或登录已过期")
    })
    public Map<String, Object> saveDailyMeasurement(HttpServletRequest request,
                                                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                            required = true,
                                                            description = "日测量上报请求体",
                                                            content = @Content(
                                                                    schema = @Schema(implementation = PatientDailyMeasurementRequest.class),
                                                                    examples = {
                                                                            @ExampleObject(
                                                                                    name = "示例",
                                                                                    value = "{\n  \"measuredAt\": \"2026-01-26 11:40:00\",\n  \"sbp\": 128,\n  \"dbp\": 82,\n  \"heartRate\": 76,\n  \"spo2\": 98,\n  \"weightKg\": 68.5,\n  \"temperatureC\": 36.6,\n  \"symptoms\": \"无明显不适\"\n}"
                                                                            )
                                                                    }
                                                            )
                                                    )
                                                    @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);

        PatientDailyMeasurement m = new PatientDailyMeasurement();
        m.setPatientId(patientId);
        m.setMeasuredAt(parseDateTime(toStr(body == null ? null : body.get("measuredAt")), LocalDateTime.now()));
        m.setSbp(toDouble(body == null ? null : body.get("sbp"), null));
        m.setDbp(toDouble(body == null ? null : body.get("dbp"), null));
        m.setHeartRate(toDouble(body == null ? null : body.get("heartRate"), body == null ? null : body.get("hr")));
        m.setWeightKg(toDouble(body == null ? null : body.get("weightKg"), body == null ? null : body.get("weight")));
        m.setTemperatureC(toDouble(body == null ? null : body.get("temperatureC"), body == null ? null : body.get("temp")));
        m.setSpo2(toDouble(body == null ? null : body.get("spo2"), null));
        m.setGlucose(toDouble(body == null ? null : body.get("glucose"), null));
        m.setGlucoseType(toStr(body == null ? null : body.get("glucoseType")));

        // sleep: 数值（小时）写入 sleep；原始文字写入 ext1，方便管理端展示
        Object sleepRaw = body == null ? null : body.get("sleep");
        m.setSleep(toDouble(sleepRaw, null));
        m.setExt1(sleepRaw == null ? null : String.valueOf(sleepRaw));

        // 今日症状与生活情况（文字型）：存到扩展字段，其他症状兼容 symptomNote -> symptoms
        Object appetiteRaw = body == null ? null : body.get("appetite");
        Object stoolRaw = body == null ? null : body.get("stool");
        Object symptomNoteRaw = body == null ? null : body.get("symptomNote");

        m.setExt2(appetiteRaw == null ? null : String.valueOf(appetiteRaw));
        m.setExt3(stoolRaw == null ? null : String.valueOf(stoolRaw));

        String symptoms = toStr(body == null ? null : body.get("symptoms"));
        if (symptoms == null || symptoms.trim().isEmpty()) {
            symptoms = symptomNoteRaw == null ? null : String.valueOf(symptomNoteRaw);
        }
        m.setSymptoms(symptoms);

        patientDailyMeasurementMapper.insert(m);

        if (m.getSbp() != null && m.getDbp() != null) {
            HealthMetric bp = new HealthMetric();
            bp.setPatientId(patientId);
            bp.setMetricType("BP");
            bp.setValue1(m.getSbp());
            bp.setValue2(m.getDbp());
            bp.setMeasuredAt(m.getMeasuredAt());
            bp.setCreatedByUserId(patientId);
            healthMetricService.addMetric(bp);
        }

        if (m.getHeartRate() != null) {
            HealthMetric hr = new HealthMetric();
            hr.setPatientId(patientId);
            hr.setMetricType("HR");
            hr.setValue1(m.getHeartRate());
            hr.setValue2(null);
            hr.setMeasuredAt(m.getMeasuredAt());
            hr.setCreatedByUserId(patientId);
            healthMetricService.addMetric(hr);
        }

        if (m.getSpo2() != null) {
            HealthMetric spo2 = new HealthMetric();
            spo2.setPatientId(patientId);
            spo2.setMetricType("SPO2");
            spo2.setValue1(m.getSpo2());
            spo2.setValue2(null);
            spo2.setMeasuredAt(m.getMeasuredAt());
            spo2.setCreatedByUserId(patientId);
            healthMetricService.addMetric(spo2);
        }

        if (m.getGlucose() != null) {
            HealthMetric glucose = new HealthMetric();
            glucose.setPatientId(patientId);
            // 固定为空腹血糖
            glucose.setMetricType("GLUCOSE_FASTING");
            glucose.setValue1(m.getGlucose());
            glucose.setValue2(null);
            glucose.setMeasuredAt(m.getMeasuredAt());
            glucose.setCreatedByUserId(patientId);
            healthMetricService.addMetric(glucose);
        }
        return ApiResponseUtil.ok(m);
    }

    @GetMapping("/labs")
    public Map<String, Object> labs(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        return ApiResponseUtil.ok(patientLabResultMapper.findByPatientId(patientId));
    }

    @GetMapping("/rehab-tasks")
    public Map<String, Object> rehabTasks(HttpServletRequest request, @RequestParam(required = false) String date) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        LocalDate targetDate = parseLocalDate(date, LocalDate.now());
        List<PatientRehabTask> tasks = getOrBuildTodayRehabTasks(patientId, targetDate, true);
        List<Map<String, Object>> taskItems = mapRehabTasks(tasks, targetDate);
        ensureOverdueReminderMessage(patientId, targetDate, taskItems);

        Map<String, Object> data = new HashMap<>();
        data.put("tasks", taskItems);
        data.put("streak", calculateRehabStreak(patientRehabTaskMapper.findByPatientId(patientId), targetDate));
        int overdueCount = 0;
        for (Map<String, Object> item : taskItems) {
            if (Boolean.TRUE.equals(item.get("overdue"))) {
                overdueCount++;
            }
        }
        data.put("overdueCount", overdueCount);
        data.put("date", String.valueOf(targetDate));
        return ApiResponseUtil.ok(data);
    }

    @PutMapping("/rehab-tasks/{id}/status")
    public Map<String, Object> updateRehabTaskStatus(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        String status = toStr(body == null ? null : body.get("status"));
        if (status == null || status.trim().isEmpty()) {
            return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "status 不能为空");
        }
        patientRehabTaskMapper.updateStatus(patientId, id, status);
        return ApiResponseUtil.ok(true);
    }

    @PutMapping("/rehab-tasks/{id}/difficulty")
    public Map<String, Object> updateRehabTaskDifficulty(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        String difficulty = toStr(body == null ? null : body.get("difficulty"));
        patientRehabTaskMapper.updateDifficulty(patientId, id, difficulty);
        return ApiResponseUtil.ok(true);
    }

    @PostMapping("/tcm-surveys")
    public Map<String, Object> submitTcmSurvey(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);

        try {
            PatientTcmSurvey s = new PatientTcmSurvey();
            s.setPatientId(patientId);

            // 1. surveyType：前端没传时，默认 DAILY
            String surveyType = toStr(body == null ? null : body.get("surveyType"));
            if (surveyType == null || surveyType.trim().isEmpty()) {
                surveyType = "DAILY";
            }
            s.setSurveyType(surveyType);

            // 2. answers：前端传的是对象，后端要转成标准 JSON 字符串
            Object answersObj = body == null ? null : body.get("answers");

            // 3. result：兼容前端当前传法
            //    前端现在传的是 risk + features + date，不是 result
            Object resultObj = body == null ? null : body.get("result");
            if (resultObj == null) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("risk", body == null ? null : body.get("risk"));
                resultMap.put("features", body == null ? null : body.get("features"));
                resultMap.put("date", body == null ? null : body.get("date"));
                resultObj = resultMap;
            }

            s.setAnswersJson(answersObj == null ? null : OBJECT_MAPPER.writeValueAsString(answersObj));
            s.setResultJson(resultObj == null ? null : OBJECT_MAPPER.writeValueAsString(resultObj));
            s.setAssessedAt(LocalDateTime.now());

            patientTcmSurveyMapper.insert(s);
            return ApiResponseUtil.ok(s);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponseUtil.fail(ApiCode.INTERNAL_ERROR, "问卷提交失败：" + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        return ApiResponseUtil.ok(userMapper.findById(patientId));
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        if (body != null) {
            String name = toStr(body.get("name"));
            String address = toStr(body.get("address"));
            Integer age = toInt(body.get("age"));
            String sex = toStr(body.get("sex"));
            String phone = toStr(body.get("phone"));
            String normalizedPhone = phone == null ? null : ValidationUtil.normalizeCnMobile(phone);
            if (normalizedPhone != null && !normalizedPhone.trim().isEmpty() && !ValidationUtil.isValidCnMobile(normalizedPhone)) {
                return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "手机号格式不合法");
            }
            userMapper.updateProfileById(patientId, name, address, age, sex, normalizedPhone);
        }
        return ApiResponseUtil.ok(userMapper.findById(patientId));
    }

    @GetMapping("/linked-accounts")
    public Map<String, Object> listLinkedAccounts(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        return ApiResponseUtil.ok(patientLinkedAccountMapper.findByPatientId(patientId));
    }

    @PostMapping("/linked-accounts")
    public Map<String, Object> addLinkedAccount(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        PatientLinkedAccount a = new PatientLinkedAccount();
        a.setPatientId(patientId);
        a.setLinkedUserId(toLong(body == null ? null : body.get("linkedUserId")));
        a.setLinkedUsername(toStr(body == null ? null : body.get("linkedUsername")));
        a.setLinkedPhone(toStr(body == null ? null : body.get("linkedPhone")));
        a.setRelation(toStr(body == null ? null : body.get("relation")));
        a.setStatus("ACTIVE");
        patientLinkedAccountMapper.insert(a);
        return ApiResponseUtil.ok(a);
    }

    @DeleteMapping("/linked-accounts/{id}")
    public Map<String, Object> deleteLinkedAccount(HttpServletRequest request, @PathVariable("id") Long id) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        patientLinkedAccountMapper.deleteById(patientId, id);
        return ApiResponseUtil.ok(true);
    }

    @GetMapping("/messages")
    public Map<String, Object> listMessages(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        LocalDate today = LocalDate.now();
        List<PatientRehabTask> tasks = getOrBuildTodayRehabTasks(patientId, today, false);
        ensureOverdueReminderMessage(patientId, today, mapRehabTasks(tasks, today));
        return ApiResponseUtil.ok(patientMessageMapper.findByPatientId(patientId));
    }

    @PutMapping("/messages/{id}/read")
    public Map<String, Object> markMessageRead(HttpServletRequest request, @PathVariable("id") Long id) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        patientMessageMapper.markRead(patientId, id);
        return ApiResponseUtil.ok(true);
    }

    @PutMapping("/messages/read-all")
    public Map<String, Object> markAllMessagesRead(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        patientMessageMapper.markAllRead(patientId);
        return ApiResponseUtil.ok(true);
    }

    @PostMapping("/feedback")
    public Map<String, Object> submitFeedback(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        PatientFeedback f = new PatientFeedback();
        f.setPatientId(patientId);
        f.setFeedbackType(toStr(body == null ? null : body.get("feedbackType")));
        f.setContent(toStr(body == null ? null : body.get("content")));
        f.setContact(toStr(body == null ? null : body.get("contact")));
        f.setStatus("NEW");
        patientFeedbackMapper.insert(f);
        return ApiResponseUtil.ok(f);
    }

    @GetMapping("/feedback")
    public Map<String, Object> listFeedback(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        return ApiResponseUtil.ok(patientFeedbackMapper.findByPatientId(patientId));
    }

    @GetMapping("/followups")
    public Map<String, Object> listFollowups(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        List<Map<String, Object>> rows = patientFollowupRecordMapper.findPatientListByPatientId(patientId);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                row.put("followupStaffName",
                        firstNonBlank(
                                toStr(row.get("followupStaffName")),
                                toStr(row.get("staffName")),
                                toStr(row.get("doctorName")),
                                "医生/护士"));
            }
        }
        return ApiResponseUtil.ok(rows);
    }

    @GetMapping("/followups/{id}")
    public Map<String, Object> getFollowupDetail(HttpServletRequest request, @PathVariable("id") Long id) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        Map<String, Object> record = patientFollowupRecordMapper.findPatientDetailById(patientId, id);
        if (record == null) {
            return ApiResponseUtil.fail(ApiCode.NOT_FOUND, "随访记录不存在");
        }
        record.put("followupStaffName",
                firstNonBlank(
                        toStr(record.get("followupStaffName")),
                        toStr(record.get("staffName")),
                        toStr(record.get("doctorName")),
                        "医生/护士"));
        return ApiResponseUtil.ok(record);
    }

    @PostMapping("/followup-applications")
    public Map<String, Object> applyFollowup(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        PatientFollowupApplication a = new PatientFollowupApplication();
        a.setPatientId(patientId);
        a.setApplyReason(toStr(body == null ? null : body.get("applyReason")));
        a.setContactPhone(toStr(body == null ? null : body.get("contactPhone")));
        a.setStatus("PENDING");
        a.setPreferredTime(parseDateTime(toStr(body == null ? null : body.get("preferredTime")), null));
        patientFollowupApplicationMapper.insert(a);
        return ApiResponseUtil.ok(a);
    }

    @GetMapping("/followup-applications")
    public Map<String, Object> listFollowupApplications(HttpServletRequest request) {
        Map<String, Object> err = requireLogin(request);
        if (err != null) {
            return err;
        }
        Long patientId = currentUserId(request);
        return ApiResponseUtil.ok(patientFollowupApplicationMapper.findByPatientId(patientId));
    }


    private List<PatientRehabTask> getOrBuildTodayRehabTasks(Long patientId, LocalDate targetDate, boolean generateIfMissing) {
        List<PatientRehabTask> all = patientRehabTaskMapper.findByPatientId(patientId);
        List<PatientRehabTask> current = filterRehabTasksByDate(all, targetDate);
        if (!current.isEmpty() || !generateIfMissing) {
            return current;
        }

        PatientDailyMeasurement todayMeasurement = latestDailyMeasurementOfDate(patientId, targetDate);
        PatientTcmSurvey todaySurvey = latestSurveyOfDate(patientId, targetDate);
        if (todayMeasurement == null && todaySurvey == null) {
            return current;
        }

        List<PatientRehabTask> generated = buildAutoRehabTasks(patientId, targetDate, todayMeasurement, todaySurvey);
        for (PatientRehabTask task : generated) {
            patientRehabTaskMapper.insert(task);
        }
        return filterRehabTasksByDate(patientRehabTaskMapper.findByPatientId(patientId), targetDate);
    }

    private List<PatientRehabTask> filterRehabTasksByDate(List<PatientRehabTask> all, LocalDate targetDate) {
        List<PatientRehabTask> result = new ArrayList<>();
        if (all == null || targetDate == null) {
            return result;
        }
        for (PatientRehabTask task : all) {
            if (task == null) {
                continue;
            }
            LocalDate due = task.getDueDate();
            LocalDate start = task.getStartDate();
            if (targetDate.equals(due) || targetDate.equals(start)) {
                result.add(task);
            }
        }
        return result;
    }

    private PatientDailyMeasurement latestDailyMeasurementOfDate(Long patientId, LocalDate targetDate) {
        List<PatientDailyMeasurement> rows = patientDailyMeasurementMapper.findInRange(patientId, targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    private PatientTcmSurvey latestSurveyOfDate(Long patientId, LocalDate targetDate) {
        List<PatientTcmSurvey> rows = patientTcmSurveyMapper.findByPatientId(patientId);
        if (rows == null) {
            return null;
        }
        for (PatientTcmSurvey row : rows) {
            if (row == null) {
                continue;
            }
            LocalDateTime ts = row.getAssessedAt() != null ? row.getAssessedAt() : row.getCreatedAt();
            if (ts != null && targetDate.equals(ts.toLocalDate())) {
                return row;
            }
        }
        return null;
    }

    private List<PatientRehabTask> buildAutoRehabTasks(Long patientId, LocalDate targetDate, PatientDailyMeasurement measurement, PatientTcmSurvey survey) {
        Map<String, Object> answers = parseSurveyAnswers(survey);
        String disease = firstNonBlank(
                toStr(answers.get("chronic_disease")),
                toStr(answers.get("diseaseType")),
                inferDiseaseFromMeasurement(measurement),
                "OTHER");

        Integer dyspneaScore = toInt(answers.get("dyspnea_score"));
        Integer fatigueScore = toInt(answers.get("fatigue_score"));
        Integer edema = toInt(answers.get("edema"));
        Integer dietScore = toInt(answers.get("diet_adherence_star"));
        Integer sugarScore = toInt(answers.get("sugar_intake_star"));
        Integer exerciseMinutes = toInt(answers.get("exercise_minutes"));

        String exerciseTitle = "餐后轻步行";
        String exerciseDesc = "建议今天安排 20 分钟轻到中等强度步行，期间以微微出汗、能正常说话为宜。";
        String exerciseIcon = "🚶";
        String exerciseCategory = "运动";
        int exerciseDuration = 20;

        if ("COPD".equalsIgnoreCase(disease)) {
            exerciseTitle = "呼吸训练 + 轻步行";
            exerciseDesc = "先做 5 分钟缩唇呼吸/腹式呼吸，再进行 10 分钟缓步行走；若出现明显气短，请立即停下休息。";
            exerciseIcon = "🫁";
            exerciseCategory = "呼吸训练";
            exerciseDuration = 15;
        } else if ("HF".equalsIgnoreCase(disease)) {
            exerciseTitle = "分段低强度活动";
            exerciseDesc = "建议今天进行 10–15 分钟低强度活动，可分 2 次完成；若下肢浮肿或胸闷明显，请以休息和呼吸训练为主。";
            exerciseIcon = "🧘";
            exerciseCategory = "运动";
            exerciseDuration = 15;
        } else if ("DM".equalsIgnoreCase(disease)) {
            exerciseTitle = "餐后步行控糖";
            exerciseDesc = "建议餐后 30 分钟内安排 20 分钟轻步行，有助于稳定血糖；如出现低血糖不适，请先补充食物再活动。";
            exerciseIcon = "🚶";
            exerciseCategory = "运动";
            exerciseDuration = 20;
        }

        if (measurement != null) {
            if ((measurement.getSpo2() != null && measurement.getSpo2() < 95) || (dyspneaScore != null && dyspneaScore >= 7)) {
                exerciseTitle = "放缓运动，先做呼吸放松";
                exerciseDesc = "根据您今天的呼吸情况，建议先做 10 分钟呼吸放松训练，再视体力决定是否进行少量步行；若症状加重请及时联系医生。";
                exerciseIcon = "🫁";
                exerciseCategory = "呼吸训练";
                exerciseDuration = 10;
            } else if ((measurement.getSbp() != null && measurement.getSbp() >= 150) || (measurement.getDbp() != null && measurement.getDbp() >= 95)) {
                exerciseDesc = "结合您今天的血压情况，建议避开剧烈运动，改为 15–20 分钟平地慢走，并注意放松情绪、避免熬夜。";
                exerciseDuration = 15;
            } else if (measurement.getGlucose() != null && measurement.getGlucose() >= 10) {
                exerciseDesc = "结合您今天的血糖情况，建议餐后安排 20 分钟轻步行，避免久坐，有助于血糖管理。";
            }
        }
        if (fatigueScore != null && fatigueScore >= 7) {
            exerciseDesc = "您今天乏力较明显，建议以 10–15 分钟轻活动或分段步行为主，量力而行，避免过度劳累。";
            exerciseDuration = Math.min(exerciseDuration, 15);
        }
        if (exerciseMinutes != null && exerciseMinutes >= 30) {
            exerciseDesc = "您今天已有一定活动量，建议晚饭后继续保持 10–15 分钟轻步行或舒展活动，帮助维持稳定状态。";
            exerciseDuration = 10;
        }

        String dietTitle = "清淡饮食管理";
        String dietDesc = "建议今天三餐定时定量，少油少盐，晚餐不过饱，减少甜食和夜宵。";
        String dietIcon = "🥗";
        String dietCategory = "饮食";
        int dietDuration = 15;

        if ("HTN".equalsIgnoreCase(disease)) {
            dietTitle = "低盐饮食执行";
            dietDesc = "结合血压管理需求，建议今天控制咸菜、酱料、外卖和腌制品摄入，烹调尽量清淡，注意补水。";
        } else if ("DM".equalsIgnoreCase(disease)) {
            dietTitle = "控糖饮食执行";
            dietDesc = "建议今天主食定量、少甜饮少零食，优先选择粗杂粮和高纤维蔬菜，避免一次性吃得过多。";
        } else if ("HF".equalsIgnoreCase(disease)) {
            dietTitle = "低盐少量多餐";
            dietDesc = "建议今天少量多餐，避免过饱和重口味食物，控制盐分摄入；如医生有液体限制，请按医嘱执行。";
        } else if ("COPD".equalsIgnoreCase(disease)) {
            dietTitle = "易消化高营养饮食";
            dietDesc = "建议今天选择温软、易消化、富含蛋白的食物，避免过冷过辣和容易胀气的饮食。";
        }

        if (measurement != null) {
            if ((measurement.getSbp() != null && measurement.getSbp() >= 150) || (measurement.getDbp() != null && measurement.getDbp() >= 95)) {
                dietDesc = "根据您今天的血压情况，建议把今天的重点放在低盐饮食上，少吃加工食品和高钠调味品。";
                dietTitle = "重点低盐饮食";
            }
            if (measurement.getGlucose() != null) {
                if (measurement.getGlucose() >= 10) {
                    dietTitle = "重点控糖饮食";
                    dietDesc = "根据您今天的血糖结果，建议严格减少含糖饮料、甜点和精制主食，三餐规律，必要时按医嘱监测血糖。";
                } else if (measurement.getGlucose() <= 3.9) {
                    dietTitle = "规律进餐防低糖";
                    dietDesc = "结合您今天的血糖情况，建议按时进餐并随身准备少量加餐，避免长时间空腹。";
                }
            }
        }
        if (dietScore != null && dietScore <= 2) {
            dietDesc = "您今天饮食控制较差，建议晚餐开始减少高油高盐和高糖食物，优先蔬菜、优质蛋白和清淡烹调。";
        }
        if (sugarScore != null && sugarScore <= 2) {
            dietTitle = "晚间控糖提醒";
            dietDesc = "根据您今天的控糖情况，建议晚间避免水果加餐和甜饮，主食适量，必要时按计划复测血糖。";
        }
        if (edema != null && edema >= 2) {
            dietDesc = "考虑到您今天浮肿较明显，建议今天尤其注意低盐饮食，少喝含糖饮料和重口味汤品，必要时记录体重变化。";
        }

        List<PatientRehabTask> tasks = new ArrayList<>();
        tasks.add(newAutoTask(patientId, targetDate, "EXERCISE", exerciseTitle, exerciseDesc, exerciseCategory, exerciseIcon, exerciseDuration, "18:00"));
        tasks.add(newAutoTask(patientId, targetDate, "DIET", dietTitle, dietDesc, dietCategory, dietIcon, dietDuration, "20:00"));
        return tasks;
    }

    private PatientRehabTask newAutoTask(Long patientId, LocalDate targetDate, String key, String title, String desc, String category, String icon, int duration, String dueTime) {
        PatientRehabTask task = new PatientRehabTask();
        task.setPatientId(patientId);
        task.setTitle(title);
        task.setDescription(desc);
        task.setDifficulty("just");
        task.setStatus("todo");
        task.setStartDate(targetDate);
        task.setDueDate(targetDate);
        task.setExt1(category);
        task.setExt2(icon);
        task.setExt3(String.valueOf(duration));
        task.setExt4(dueTime);
        task.setExt5("AUTO:" + targetDate + ":" + key);
        return task;
    }

    private List<Map<String, Object>> mapRehabTasks(List<PatientRehabTask> tasks, LocalDate targetDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (tasks == null) {
            return list;
        }
        for (PatientRehabTask task : tasks) {
            if (task == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("title", firstNonBlank(task.getTitle(), "今日康养任务"));
            item.put("desc", firstNonBlank(task.getDescription(), "请按建议完成今日康养安排。"));
            item.put("description", task.getDescription());
            item.put("category", firstNonBlank(task.getExt1(), guessTaskCategory(task.getTitle())));
            item.put("icon", firstNonBlank(task.getExt2(), guessTaskIcon(task.getTitle())));
            item.put("duration", parsePositiveInt(task.getExt3(), 15));
            item.put("difficulty", firstNonBlank(task.getDifficulty(), "just"));
            item.put("status", normalizeTaskStatus(task.getStatus()));
            item.put("dueTime", firstNonBlank(task.getExt4(), "20:00"));
            item.put("dueDate", task.getDueDate() == null ? null : String.valueOf(task.getDueDate()));
            boolean overdue = isTaskOverdue(task, targetDate);
            item.put("overdue", overdue);
            item.put("deadlineText", overdue ? "已超过建议完成时间" : ("建议在 " + firstNonBlank(task.getExt4(), "20:00") + " 前完成"));
            list.add(item);
        }
        return list;
    }

    private int calculateRehabStreak(List<PatientRehabTask> tasks, LocalDate targetDate) {
        if (tasks == null || tasks.isEmpty() || targetDate == null) {
            return 0;
        }
        int streak = 0;
        LocalDate cursor = targetDate;
        while (true) {
            List<PatientRehabTask> oneDay = filterRehabTasksByDate(tasks, cursor);
            if (oneDay.isEmpty()) {
                break;
            }
            boolean allDone = true;
            for (PatientRehabTask task : oneDay) {
                if (!"done".equals(normalizeTaskStatus(task == null ? null : task.getStatus()))) {
                    allDone = false;
                    break;
                }
            }
            if (!allDone) {
                break;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private void ensureOverdueReminderMessage(Long patientId, LocalDate targetDate, List<Map<String, Object>> taskItems) {
        if (patientId == null || targetDate == null || taskItems == null || taskItems.isEmpty()) {
            return;
        }
        int overdueCount = 0;
        for (Map<String, Object> item : taskItems) {
            if (item != null && Boolean.TRUE.equals(item.get("overdue")) && !"done".equals(item.get("status"))) {
                overdueCount++;
            }
        }
        if (overdueCount <= 0) {
            return;
        }
        String marker = "REHAB_OVERDUE:" + targetDate;
        List<PatientMessage> messages = patientMessageMapper.findByPatientId(patientId);
        if (messages != null) {
            for (PatientMessage msg : messages) {
                if (msg != null && marker.equals(toStr(msg.getExt1()))) {
                    return;
                }
            }
        }
        PatientMessage message = new PatientMessage();
        message.setPatientId(patientId);
        message.setTitle("今日康养任务提醒");
        message.setContent(overdueCount == 1
                ? "您今天有 1 项康养任务已超过建议完成时间，请尽快完成；若身体不适，请及时联系医生。"
                : "您今天有 " + overdueCount + " 项康养任务已超过建议完成时间，请优先完成饮食或轻运动任务；若身体不适，请及时联系医生。");
        message.setMessageType("SYSTEM");
        message.setStatus("NEW");
        message.setExt1(marker);
        message.setExt2("康养任务");
        patientMessageMapper.insert(message);
    }

    private Map<String, Object> parseSurveyAnswers(PatientTcmSurvey survey) {
        Map<String, Object> answers = new HashMap<>();
        if (survey == null || survey.getAnswersJson() == null || survey.getAnswersJson().trim().isEmpty()) {
            return answers;
        }
        try {
            Object parsed = OBJECT_MAPPER.readValue(survey.getAnswersJson(), Object.class);
            if (parsed instanceof Map<?, ?>) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) parsed).entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    answers.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception ignored) {
        }
        return answers;
    }

    private PatientMessage firstDoctorMessage(List<PatientMessage> messages) {
        if (messages == null) {
            return null;
        }
        for (PatientMessage message : messages) {
            if (message == null) {
                continue;
            }
            String type = toStr(message.getMessageType());
            String t = type == null ? "" : type.trim().toUpperCase();
            if (t.isEmpty() || "DOCTOR".equals(t) || "ADVICE".equals(t) || "RECOMMENDATION".equals(t)) {
                return message;
            }
        }
        return null;
    }

    private String inferDiseaseFromMeasurement(PatientDailyMeasurement measurement) {
        if (measurement == null) {
            return null;
        }
        if (measurement.getGlucose() != null) {
            return "DM";
        }
        if ((measurement.getSpo2() != null && measurement.getSpo2() < 96) || firstNonBlank(measurement.getSymptoms()) != null && firstNonBlank(measurement.getSymptoms()).contains("咳")) {
            return "COPD";
        }
        if (measurement.getSbp() != null || measurement.getDbp() != null) {
            return "HTN";
        }
        return null;
    }

    private String normalizeTaskStatus(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        return ("DONE".equals(s) || "COMPLETED".equals(s)) ? "done" : "todo";
    }

    private boolean isTaskOverdue(PatientRehabTask task, LocalDate targetDate) {
        if (task == null || targetDate == null) {
            return false;
        }
        if ("done".equals(normalizeTaskStatus(task.getStatus()))) {
            return false;
        }
        LocalDate dueDate = task.getDueDate() != null ? task.getDueDate() : task.getStartDate();
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return true;
        }
        if (dueDate == null || !dueDate.equals(LocalDate.now())) {
            return false;
        }
        Integer dueMinutes = parseTimeToMinutes(task.getExt4());
        if (dueMinutes == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        int currentMinutes = now.getHour() * 60 + now.getMinute();
        return currentMinutes > dueMinutes;
    }

    private Integer parseTimeToMinutes(String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) {
            return null;
        }
        String[] parts = timeText.trim().split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 60 + minute;
        } catch (Exception e) {
            return null;
        }
    }

    private int parsePositiveInt(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            int n = Integer.parseInt(text.trim());
            return n > 0 ? n : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String guessTaskCategory(String title) {
        String text = title == null ? "" : title;
        if (text.contains("饮食") || text.contains("控糖") || text.contains("低盐")) {
            return "饮食";
        }
        if (text.contains("呼吸")) {
            return "呼吸训练";
        }
        return "运动";
    }

    private String guessTaskIcon(String title) {
        String text = title == null ? "" : title;
        if (text.contains("饮食") || text.contains("控糖") || text.contains("低盐")) {
            return "🥗";
        }
        if (text.contains("呼吸")) {
            return "🫁";
        }
        if (text.contains("拉伸") || text.contains("舒展")) {
            return "🧘";
        }
        return "🚶";
    }

    private LocalDate parseLocalDate(String text, LocalDate fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }


    private static String normalizeRiskLevel(String level) {
        if (level == null) {
            return "mid";
        }
        String s = level.trim().toUpperCase();
        if (s.contains("HIGH") || s.contains("高")) {
            return "high";
        }
        if (s.contains("LOW") || s.contains("低")) {
            return "low";
        }
        return "mid";
    }

    private static String riskText(String level) {
        String normalized = normalizeRiskLevel(level);
        if ("high".equals(normalized)) {
            return "高风险";
        }
        if ("low".equals(normalized)) {
            return "低风险";
        }
        return "中风险";
    }

    private static String firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private static String toStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Double toDouble(Object v1, Object v2) {
        Double a = parseDouble(v1);
        if (a != null) {
            return a;
        }
        return parseDouble(v2);
    }

    private static Double parseDouble(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(String s, LocalDateTime fallback) {
        if (s == null || s.trim().isEmpty()) {
            return fallback;
        }

        String text = s.trim();

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(text, formatter);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
        }

        return fallback;
    }
}
