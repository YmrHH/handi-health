package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.DashboardSyndromeAbnormalPatient;
import com.example.zhinengsuifang.entity.PatientBasicInfo;
import com.example.zhinengsuifang.entity.PatientDailyMeasurement;
import com.example.zhinengsuifang.entity.PatientTcmSurvey;
import com.example.zhinengsuifang.entity.TCMDiagnosis;
import com.example.zhinengsuifang.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.zhinengsuifang.mapper.AlertCenterMapper;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.PatientBasicInfoMapper;
import com.example.zhinengsuifang.mapper.PatientDailyMeasurementMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupRecordMapper;
import com.example.zhinengsuifang.mapper.PatientTcmSurveyMapper;
import com.example.zhinengsuifang.mapper.TCMDiagnosisMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/patient")
@Tag(name = "患者", description = "患者相关接口（首页与管理）")
public class ApiPatientController {

    private static final Set<String> ALLOWED_RISK_LEVELS = Set.of("HIGH", "MID", "LOW");
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("^[\\p{IsHan}A-Za-z0-9]+$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private DashboardService dashboardService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PatientBasicInfoMapper patientBasicInfoMapper;

    @Resource
    private PatientDailyMeasurementMapper patientDailyMeasurementMapper;

    @Resource
    private PatientTcmSurveyMapper patientTcmSurveyMapper;

    @Resource
    private PatientFollowupRecordMapper patientFollowupRecordMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private AlertCenterMapper alertCenterMapper;

    @Resource
    private TCMDiagnosisMapper tcmDiagnosisMapper;

    @GetMapping("/risk-list")
    public Map<String, Object> riskList(@RequestParam(required = false) String level,
                                        @RequestParam(required = false) String disease,
                                        @RequestParam(required = false) String syndrome,
                                        @RequestParam(required = false) String doctor,
                                        @RequestParam(required = false, name = "q") String keyword,
                                        @RequestParam(required = false) Integer pageNo,
                                        @RequestParam(required = false) Integer pageSize) {
        int p = pageNo == null ? 1 : Math.max(1, pageNo);
        int ps = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 200));

        Map<String, Object> data = new HashMap<>();
        data.put("levelParam", level);
        data.put("diseaseParam", disease);
        data.put("syndromeParam", syndrome);
        data.put("doctorParam", doctor);
        data.put("keyword", keyword);
        data.put("diseaseList", new ArrayList<>());
        data.put("syndromeList", new ArrayList<>());
        List<Map<String, Object>> rawDoctors = userMapper.selectDoctorsForOrgUser();
        List<Map<String, Object>> doctorList = new ArrayList<>();
        if (rawDoctors != null) {
            for (Map<String, Object> r : rawDoctors) {
                if (r == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>(r);
                Object id = item.get("id");
                Object name = item.get("name");
                Object username = item.get("username");
                String display = null;
                if (name != null && !String.valueOf(name).trim().isEmpty()) {
                    display = String.valueOf(name).trim();
                } else if (username != null && !String.valueOf(username).trim().isEmpty()) {
                    display = String.valueOf(username).trim();
                }
                item.put("value", id);
                item.put("label", display == null ? "" : display);
                item.put("text", display == null ? "" : display);
                doctorList.add(item);
            }
        }
        data.put("doctorList", doctorList);
        data.put("diseaseList", patientBasicInfoMapper.selectDistinctDiseases());
        data.put("syndromeList", patientBasicInfoMapper.selectDistinctSyndromes());
        data.put("highCount", 0);
        data.put("midCount", 0);
        data.put("lowCount", 0);
        data.put("newHighThisWeek", 0);
        data.put("pageNo", p);
        data.put("pageSize", ps);
        data.put("total", 0);
        data.put("totalPages", 0);
        data.put("rows", new ArrayList<>());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(@RequestParam(required = false) Long riskId) {
        Map<String, Object> result = new HashMap<>();
        if (riskId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: riskId");
            return result;
        }

        // 当前前端 riskId 语义等同 patientId（patient summary 中 riskId=u.id）
        Long patientId = riskId;
        User patient = userMapper.findPatientById(patientId);
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        List<Map<String, Object>> recentAlerts = alertCenterMapper.selectAlertCenterPage(patientId, null, null, null, null,
                null, null, 0, 1);
        String lastAlertText = "";
        if (recentAlerts != null && !recentAlerts.isEmpty() && recentAlerts.get(0) != null) {
            Object summary = recentAlerts.get(0).get("summary");
            lastAlertText = summary == null ? "" : String.valueOf(summary);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("riskId", riskId);
        data.put("patientId", patient.getId());
        data.put("name", safeStr(patient.getName()));
        data.put("age", patient.getAge() == null ? 0 : patient.getAge());
        PatientBasicInfo basicInfo = patientBasicInfoMapper.findByPatientId(patientId);
        data.put("disease", basicInfo == null ? "" : safeStr(basicInfo.getExt3()));
        data.put("syndrome", basicInfo == null ? "" : safeStr(basicInfo.getExt4()));
        Map<String, String> ext5 = parsePatientExt5(basicInfo);
        data.put("constitution", safeStr(ext5.get("constitution")));
        data.put("familyHistory", safeStr(ext5.get("familyHistory")));
        data.put("pastHistory", safeStr(ext5.get("pastHistory")));
        data.put("lifestyle", basicInfo == null ? "" : safeStr(basicInfo.getExt1()));
        data.put("riskLevel", safeStr(patient.getRiskLevel()));
        data.put("riskText", riskText(patient.getRiskLevel()));
        data.put("doctor", safeStr(userMapper.findPatientResponsibleDoctorName(patientId)));
        data.put("lastAlertText", lastAlertText);
        data.put("events", new ArrayList<>());
        
        // 获取中医四诊数据
        TCMDiagnosis latestTCM = tcmDiagnosisMapper.selectLatestByPatientId(patientId);
        if (latestTCM != null) {
            data.put("mainComplaint", safeStr(latestTCM.getMainComplaint()));
            data.put("tongue", safeStr(latestTCM.getTongueDescription()));
            data.put("pulse", safeStr(latestTCM.getPulseDescription()));
            data.put("tcmSummary", safeStr(latestTCM.getTcmSummary()));
        } else {
            data.put("mainComplaint", "");
            data.put("tongue", "");
            data.put("pulse", "");
            data.put("tcmSummary", "");
        }
        
        // AI风险评估（模拟实现）
        data.put("aiScore", calculateAIScore(patient, latestTCM));
        data.put("aiFocus", generateAIFocus(patient, latestTCM));
        data.put("aiAdvice", generateAIAdvice(patient, latestTCM));

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics(@RequestParam(required = false) Long riskId) {
        Map<String, Object> result = new HashMap<>();
        if (riskId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: riskId");
            return result;
        }

        Long patientId = riskId;
        User patient = userMapper.findPatientById(patientId);
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        int limit = 30;
        List<PatientDailyMeasurement> ms = patientDailyMeasurementMapper.findRecent(patientId, limit);
        List<String> dates = new ArrayList<>();
        List<Double> sbp = new ArrayList<>();
        List<Double> dbp = new ArrayList<>();
        List<Double> heartRate = new ArrayList<>();
        List<Double> spo2 = new ArrayList<>();
        String latestDate = null;

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (ms != null && !ms.isEmpty()) {
            for (int i = ms.size() - 1; i >= 0; i--) {
                PatientDailyMeasurement m = ms.get(i);
                if (m == null || m.getMeasuredAt() == null) {
                    continue;
                }
                dates.add(m.getMeasuredAt().toLocalDate().format(df));
                sbp.add(m.getSbp());
                dbp.add(m.getDbp());
                heartRate.add(m.getHeartRate());
                spo2.add(m.getSpo2());
            }
            PatientDailyMeasurement latest = ms.get(0);
            if (latest != null && latest.getMeasuredAt() != null) {
                latestDate = latest.getMeasuredAt().toLocalDate().format(df);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("dates", dates);
        data.put("sbp", sbp);
        data.put("dbp", dbp);
        data.put("heartRate", heartRate);
        data.put("spo2", spo2);
        data.put("latestDate", latestDate);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/daily-measurement/latest")
    public Map<String, Object> latestDailyMeasurement(@RequestParam(required = false) Long patientId) {
        Map<String, Object> result = new HashMap<>();
        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: patientId");
            return result;
        }

        PatientDailyMeasurement m = patientDailyMeasurementMapper.findLatest(patientId);
        Map<String, Object> data = new HashMap<>();
        if (m != null) {
            data.put("measuredAt", m.getMeasuredAt() == null ? null : m.getMeasuredAt().toString().replace('T', ' '));
            data.put("sbp", m.getSbp());
            data.put("dbp", m.getDbp());
            data.put("heartRate", m.getHeartRate());
            data.put("spo2", m.getSpo2());
            data.put("weightKg", m.getWeightKg());
            data.put("sleepHours", m.getSleep());
            data.put("symptoms", m.getSymptoms());
            data.put("sleepText", m.getExt1());
            data.put("appetiteText", m.getExt2());
            data.put("stoolText", m.getExt3());
            data.put("temperatureC", m.getTemperatureC());
            data.put("glucose", m.getGlucose());
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private static String surveyToString(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static String surveyDiseaseLabel(Object codeObj) {
        String code = surveyToString(codeObj);
        if (code == null) return null;
        return switch (code) {
            case "HTN" -> "高血压";
            case "DM" -> "糖尿病";
            case "HF" -> "冠心病/心衰";
            case "COPD" -> "慢阻肺/哮喘";
            case "OTHER" -> "其他/不确定";
            default -> code;
        };
    }

    private static String surveyStep4Title(String diseaseCode) {
        if (diseaseCode == null) return "第4步：用药、饮食与慢病管理";
        return switch (diseaseCode) {
            case "HTN" -> "第4步：用药、饮食与血压";
            case "DM" -> "第4步：用药、饮食与血糖";
            case "HF" -> "第4步：用药、饮食与心衰症状";
            case "COPD" -> "第4步：用药、饮食与呼吸管理";
            default -> "第4步：用药、饮食与慢病管理";
        };
    }

    private static Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value == null) continue;
            if (value instanceof String && ((String) value).trim().isEmpty()) continue;
            return value;
        }
        return null;
    }

    @GetMapping("/questionnaire")
    public Map<String, Object> questionnaire(@RequestParam(required = false) Long patientId) {
        Map<String, Object> result = new HashMap<>();
        if (patientId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: patientId");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("fillTime", null);
        data.put("displayNote", "选择题按小程序原选项文字展示；评分题保留原分值，前端会补充程度说明。");
        data.put("diseaseCode", null);
        data.put("diseaseLabel", null);
        data.put("step4Title", "第4步：用药、饮食与慢病管理");
        data.put("step1", new HashMap<>());
        data.put("step2", new HashMap<>());
        data.put("step3", new HashMap<>());
        data.put("step4", new HashMap<>());

        try {
            List<PatientTcmSurvey> rows = patientTcmSurveyMapper.findByPatientId(patientId);
            PatientTcmSurvey latest = (rows == null || rows.isEmpty()) ? null : rows.get(0);

            PatientBasicInfo pbi = patientBasicInfoMapper.findByPatientId(patientId);
            Double heightCm = pbi == null ? null : pbi.getHeightCm();
            Double baselineWeight = pbi == null ? null : pbi.getBaselineWeightKg();

            if (latest != null) {
                if (latest.getAssessedAt() != null) {
                    data.put("fillTime", latest.getAssessedAt().toString().replace('T', ' '));
                } else if (latest.getCreatedAt() != null) {
                    data.put("fillTime", latest.getCreatedAt().toString().replace('T', ' '));
                }

                Map<String, Object> answers = new HashMap<>();
                String answersJson = latest.getAnswersJson();
                if (answersJson != null && !answersJson.trim().isEmpty()) {
                    Object parsed = OBJECT_MAPPER.readValue(answersJson, Object.class);
                    if (parsed instanceof Map<?, ?>) {
                        for (Map.Entry<?, ?> e : ((Map<?, ?>) parsed).entrySet()) {
                            if (e.getKey() == null) continue;
                            answers.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }

                Object diseaseCode = firstNonNull(answers.get("chronic_disease"), answers.get("diseaseType"));
                String diseaseCodeText = surveyToString(diseaseCode);
                data.put("diseaseCode", diseaseCodeText);
                data.put("diseaseLabel", surveyDiseaseLabel(diseaseCode));
                data.put("step4Title", surveyStep4Title(diseaseCodeText));

                Map<String, Object> s1 = new HashMap<>();
                s1.put("chronicDiseaseType", diseaseCode);
                s1.put("overallFeeling", firstNonNull(answers.get("overall_star"), answers.get("overallFeeling"), answers.get("overallScore")));
                s1.put("fatigueLevel", firstNonNull(answers.get("fatigue_score"), answers.get("fatigueLevel"), answers.get("fatigueScore")));
                Object height = firstNonNull(answers.get("height_cm"), answers.get("height"));
                Object weight = firstNonNull(answers.get("weight_kg"), answers.get("weight"));
                if (height == null && heightCm != null) height = heightCm;
                if (weight == null && baselineWeight != null) weight = baselineWeight;
                s1.put("height", height);
                s1.put("weight", weight);
                data.put("step1", s1);

                Map<String, Object> s2 = new HashMap<>();
                s2.put("shortnessOfBreath", firstNonNull(answers.get("dyspnea_score"), answers.get("shortnessOfBreath"), answers.get("dyspneaLevel")));
                s2.put("chestTightness", firstNonNull(answers.get("chest_tightness"), answers.get("chestTightness"), answers.get("chestDiscomfort")));
                s2.put("palpitations", firstNonNull(answers.get("palpitation_score"), answers.get("palpitations"), answers.get("palpitationsLevel")));
                s2.put("legSwelling", firstNonNull(answers.get("edema"), answers.get("legSwelling"), answers.get("edemaLevel")));
                data.put("step2", s2);

                Map<String, Object> s3 = new HashMap<>();
                s3.put("sleepQuality", firstNonNull(answers.get("sleep_star"), answers.get("sleepQuality"), answers.get("sleep")));
                s3.put("nightAwakenings", firstNonNull(answers.get("night_wake"), answers.get("nightAwakenings")));
                s3.put("mood", firstNonNull(answers.get("mood_star"), answers.get("mood"), answers.get("emotion")));
                s3.put("appetite", firstNonNull(answers.get("appetite_star"), answers.get("appetite")));
                s3.put("stool", firstNonNull(answers.get("stool"), answers.get("stoolSituation")));
                data.put("step3", s3);

                Map<String, Object> s4 = new HashMap<>();
                s4.put("medicationAdherence", firstNonNull(answers.get("med_adherence_star"), answers.get("medicationAdherence"), answers.get("tookMedicineOnTime")));
                s4.put("dietControl", firstNonNull(answers.get("diet_adherence_star"), answers.get("dietControl"), answers.get("dietControlSituation")));
                s4.put("exerciseMinutes", firstNonNull(answers.get("exercise_minutes"), answers.get("exerciseMinutes"), answers.get("exerciseDuration")));

                s4.put("headacheLevel", firstNonNull(answers.get("dizzy_score"), answers.get("headacheLevel"), answers.get("dizziness")));
                s4.put("measuredBloodPressure", firstNonNull(answers.get("bp_measure"), answers.get("measuredBloodPressure"), answers.get("bpMeasured")));
                s4.put("saltIntake", firstNonNull(answers.get("salt_intake"), answers.get("saltIntake"), answers.get("dietSaltiness")));

                s4.put("glucoseMeasure", firstNonNull(answers.get("glucose_measure"), answers.get("glucoseMeasure")));
                s4.put("hypoSign", firstNonNull(answers.get("hypo_sign"), answers.get("hypoSign")));
                s4.put("sugarIntake", firstNonNull(answers.get("sugar_intake_star"), answers.get("sugarIntake")));

                s4.put("orthopnea", firstNonNull(answers.get("orthopnea"), answers.get("orthopneaLevel")));
                s4.put("nocturia", firstNonNull(answers.get("nocturia"), answers.get("nocturiaLevel")));
                s4.put("chestPainScore", firstNonNull(answers.get("chest_pain_score"), answers.get("chestPainScore")));

                s4.put("coughScore", firstNonNull(answers.get("cough_score"), answers.get("coughScore")));
                s4.put("sputum", firstNonNull(answers.get("sputum"), answers.get("sputumLevel")));
                s4.put("wheeze", firstNonNull(answers.get("wheeze"), answers.get("wheezeLevel")));
                s4.put("inhalerUse", firstNonNull(answers.get("inhaler_star"), answers.get("inhalerUse")));

                s4.put("otherNote", firstNonNull(answers.get("other_note"), answers.get("otherNote")));
                s4.put("remark", firstNonNull(answers.get("extra_note"), answers.get("remark"), answers.get("extraRemark")));
                s4.put("title", surveyStep4Title(diseaseCodeText));
                data.put("step4", s4);
            } else {
                Map<String, Object> s1 = new HashMap<>();
                if (heightCm != null) s1.put("height", heightCm);
                if (baselineWeight != null) s1.put("weight", baselineWeight);
                data.put("step1", s1);
            }
        } catch (Exception ignored) {
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/detail")
    public Map<String, Object> detailByQuery(@RequestParam(required = false) Long patientId) {
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

        PatientBasicInfo basicInfo = patientBasicInfoMapper.findByPatientId(patientId);
        Map<String, String> ext5 = parsePatientExt5(basicInfo);

        Map<String, Object> patientDto = new HashMap<>();
        patientDto.put("id", patient.getId());
        patientDto.put("name", patient.getName());
        patientDto.put("gender", patient.getSex());
        patientDto.put("age", patient.getAge());
        patientDto.put("phone", patient.getPhone());
        patientDto.put("address", patient.getAddress());
        patientDto.put("disease", basicInfo == null ? null : basicInfo.getExt3());
        patientDto.put("syndrome", basicInfo == null ? null : basicInfo.getExt4());
        patientDto.put("idCard", basicInfo == null ? null : basicInfo.getIdCard());
        patientDto.put("emergencyContact", basicInfo == null ? null : basicInfo.getEmergencyContactName());
        patientDto.put("emergencyPhone", basicInfo == null ? null : basicInfo.getEmergencyContactPhone());
        patientDto.put("insuranceType", basicInfo == null ? null : basicInfo.getInsuranceType());
        patientDto.put("insuranceNo", null);
        patientDto.put("medicalRecordNo", null);
        patientDto.put("admissionDate", null);
        patientDto.put("dischargeDate", null);
        patientDto.put("constitution", ext5.get("constitution"));
        patientDto.put("familyHistory", ext5.get("familyHistory"));
        patientDto.put("pastHistory", ext5.get("pastHistory"));
        patientDto.put("lifestyle", basicInfo == null ? null : basicInfo.getExt1());
        patientDto.put("physicalExamSummary", null);
        patientDto.put("responsibleDoctor", userMapper.findPatientResponsibleDoctorName(patientId));

        Map<String, Object> risk = new HashMap<>();
        risk.put("id", patientId);
        risk.put("riskLevel", patient.getRiskLevel());

        Long pendingCount = followUpTaskMapper.countPendingByPatient(patientId);
        Long overdueCount = followUpTaskMapper.countOverdueByPatient(patientId);
        LocalDateTime lastFollowTime = userMapper.findPatientLastFollowTime(patientId);

        LocalDateTime startAt = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime endAt = LocalDate.now().plusDays(1).atStartOfDay();
        Long alert7d = alertCenterMapper.countAlertCenter(patientId, null, null, null, null, startAt, endAt);

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", pendingCount == null ? 0L : pendingCount);
        stats.put("overdueCount", overdueCount == null ? 0L : overdueCount);
        stats.put("lastFollowupTime", lastFollowTime == null ? null : lastFollowTime.toString().replace('T', ' '));
        stats.put("alert7d", alert7d == null ? 0L : alert7d);

        List<Map<String, Object>> pendingTasks = followUpTaskMapper.selectPendingTasksByPatient(patientId, 20);
        List<Map<String, Object>> recentFollowupsRaw = patientFollowupRecordMapper.findRecentByPatientId(patientId, 5);
        List<Map<String, Object>> recentAlerts = alertCenterMapper.selectAlertCenterPage(patientId, null, null, null, null, null, null, 0, 5);

        List<Map<String, Object>> mappedFollowups = new ArrayList<>();
        if (recentFollowupsRaw != null) {
            for (Map<String, Object> f : recentFollowupsRaw) {
                if (f == null) {
                    continue;
                }
                Map<String, Object> x = new HashMap<>(f);
                String resultStatusCode = firstNonBlank(f.get("resultStatus"), f.get("result_status"));
                x.put("resultStatusCode", resultStatusCode);
                x.put("resultStatus", mapFollowupResultText(resultStatusCode));
                Object nextPlan = f.get("nextFollowupDate") != null ? f.get("nextFollowupDate") : f.get("next_followup_date");
                x.put("nextPlanTime", formatDateTime(nextPlan));
                Object followupTime = f.get("followupTime") != null ? f.get("followupTime") : f.get("followup_time");
                x.put("followupTime", formatDateTime(followupTime));
                mappedFollowups.add(x);
            }
        }

        List<Map<String, Object>> mappedAlerts = new ArrayList<>();
        if (recentAlerts != null) {
            for (Map<String, Object> a : recentAlerts) {
                if (a == null) {
                    continue;
                }
                Map<String, Object> x = new HashMap<>();
                String sourceCode = firstNonBlank(a.get("source"));
                String severityCode = firstNonBlank(a.get("level"));
                String statusCode = firstNonBlank(a.get("status"));
                x.put("id", a.get("id"));
                x.put("alertTypeCode", sourceCode);
                x.put("alertType", mapAlertSourceText(sourceCode, a.get("summary")));
                x.put("severityCode", severityCode);
                x.put("severity", mapSeverityText(severityCode));
                x.put("duration", "");
                Object t = a.get("alertTime");
                x.put("lastTime", t == null ? "" : String.valueOf(t).replace('T', ' '));
                x.put("statusCode", statusCode);
                x.put("status", mapAlertStatusText(statusCode));
                x.put("summary", a.get("summary"));
                mappedAlerts.add(x);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("patientId", patientId);
        data.put("patient", patientDto);
        data.put("risk", risk);
        data.put("stats", stats);
        data.put("pendingTasks", pendingTasks == null ? Collections.emptyList() : pendingTasks);
        data.put("recentFollowups", mappedFollowups);
        data.put("recentAlerts", mappedAlerts);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private static String safeStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Map<String, String> parsePatientExt5(PatientBasicInfo basicInfo) {
        Map<String, String> out = new HashMap<>();
        out.put("constitution", "");
        out.put("familyHistory", "");
        out.put("pastHistory", "");
        if (basicInfo == null || basicInfo.getExt5() == null || basicInfo.getExt5().trim().isEmpty()) {
            return out;
        }
        try {
            Object obj = OBJECT_MAPPER.readValue(basicInfo.getExt5(), Object.class);
            if (obj instanceof Map<?, ?>) {
                Map<?, ?> m = (Map<?, ?>) obj;
                Object constitution = m.get("constitution");
                Object familyHistory = m.get("familyHistory");
                Object pastHistory = m.get("pastHistory");
                if (constitution != null) {
                    out.put("constitution", String.valueOf(constitution));
                }
                if (familyHistory != null) {
                    out.put("familyHistory", String.valueOf(familyHistory));
                }
                if (pastHistory != null) {
                    out.put("pastHistory", String.valueOf(pastHistory));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String riskText(String level) {
        if (level == null) {
            return "风险未知";
        }
        String s = level.trim().toUpperCase();
        if (s.contains("HIGH") || s.contains("高")) {
            return "高危";
        }
        if (s.contains("MID") || s.contains("MED") || s.contains("中")) {
            return "中危";
        }
        if (s.contains("LOW") || s.contains("低")) {
            return "低危";
        }
        return level;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("patientId", null);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "创建成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/register-miniprogram")
    public Map<String, Object> registerMiniProgram(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        String phone = body == null ? null : String.valueOf(body.get("phone"));
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "注册成功");
        Map<String, Object> data = new HashMap<>();
        data.put("username", phone);
        result.put("data", data);
        return result;
    }

    // -----------------------------
    // Web 管理端：患者档案编辑保存（与前端 patientApi 对齐）
    // -----------------------------

    @PostMapping("/update-basic-info")
    public Map<String, Object> updateBasicInfo(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long patientId = toLong(body == null ? null : body.get("patientId"));
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

        String name = safeTrim(body.get("name"));
        String gender = safeTrim(body.get("gender"));
        Integer age = toInt(body.get("age"));
        String phone = safeTrim(body.get("phone"));
        String address = safeTrim(body.get("address"));
        String idCard = safeTrim(body.get("idCard"));
        String familyHistory = safeTrim(body.get("familyHistory"));

        // user 表：姓名/性别/年龄/电话/住址
        userMapper.updateProfileById(patientId,
                name.isEmpty() ? patient.getName() : name,
                address.isEmpty() ? patient.getAddress() : address,
                age == null ? patient.getAge() : age,
                gender.isEmpty() ? patient.getSex() : gender,
                phone.isEmpty() ? patient.getPhone() : phone
        );

        // patient_basic_info：身份证 + ext5(JSON) 里的 familyHistory（同时兼容 profile 读取）
        PatientBasicInfo pbi = patientBasicInfoMapper.findByPatientId(patientId);
        if (pbi == null) {
            pbi = new PatientBasicInfo();
            pbi.setPatientId(patientId);
        }
        if (!idCard.isEmpty()) {
            pbi.setIdCard(idCard);
        }

        Map<String, String> ext5 = parsePatientExt5(pbi);
        if (!familyHistory.isEmpty()) {
            ext5.put("familyHistory", familyHistory);
        }
        pbi.setExt5(writeJsonSafe(ext5));

        // insert / update
        if (patientBasicInfoMapper.findByPatientId(patientId) == null) {
            patientBasicInfoMapper.insert(pbi);
        } else {
            patientBasicInfoMapper.updateByPatientId(pbi);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "更新成功");
        result.put("data", true);
        return result;
    }

    @PostMapping("/update-diagnosis")
    public Map<String, Object> updateDiagnosis(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long patientId = toLong(body == null ? null : body.get("patientId"));
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

        String disease = safeTrim(body.get("disease"));
        String syndrome = safeTrim(body.get("syndrome"));
        String constitution = safeTrim(body.get("constitution"));

        PatientBasicInfo pbi = patientBasicInfoMapper.findByPatientId(patientId);
        if (pbi == null) {
            pbi = new PatientBasicInfo();
            pbi.setPatientId(patientId);
        }

        // ext3=病种，ext4=证型（与现有查询保持一致）
        if (!disease.isEmpty()) pbi.setExt3(disease);
        if (!syndrome.isEmpty()) pbi.setExt4(syndrome);

        // constitution 放入 ext5 JSON
        Map<String, String> ext5 = parsePatientExt5(pbi);
        if (!constitution.isEmpty()) {
            ext5.put("constitution", constitution);
        }
        pbi.setExt5(writeJsonSafe(ext5));

        if (patientBasicInfoMapper.findByPatientId(patientId) == null) {
            patientBasicInfoMapper.insert(pbi);
        } else {
            patientBasicInfoMapper.updateByPatientId(pbi);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "更新成功");
        result.put("data", true);
        return result;
    }

    @PostMapping("/update-risk-owner")
    public Map<String, Object> updateRiskOwner(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long patientId = toLong(body == null ? null : body.get("patientId"));
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

        String riskLevel = safeTrim(body.get("riskLevel")).toUpperCase();
        String responsibleDoctor = safeTrim(body.get("responsibleDoctor"));

        if (!riskLevel.isEmpty() && !ALLOWED_RISK_LEVELS.contains(riskLevel)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "riskLevel 参数不合法，仅支持 HIGH/MID/LOW");
            return result;
        }

        if (!riskLevel.isEmpty()) {
            userMapper.updateRiskLevelById(patientId, riskLevel);
        }

        // 责任医生名称：写入 patient_basic_info.ext2（summary 查询口径就是 ext2）
        PatientBasicInfo pbi = patientBasicInfoMapper.findByPatientId(patientId);
        if (pbi == null) {
            pbi = new PatientBasicInfo();
            pbi.setPatientId(patientId);
        }
        if (!responsibleDoctor.isEmpty()) {
            pbi.setExt2(responsibleDoctor);
        }
        if (patientBasicInfoMapper.findByPatientId(patientId) == null) {
            patientBasicInfoMapper.insert(pbi);
        } else {
            patientBasicInfoMapper.updateByPatientId(pbi);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "更新成功");
        result.put("data", true);
        return result;
    }

    @PostMapping("/update-lifestyle")
    public Map<String, Object> updateLifestyle(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long patientId = toLong(body == null ? null : body.get("patientId"));
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

        String lifestyle = safeTrim(body.get("lifestyle"));
        String pastHistory = safeTrim(body.get("pastHistory"));
        String familyHistory = safeTrim(body.get("familyHistory"));

        PatientBasicInfo pbi = patientBasicInfoMapper.findByPatientId(patientId);
        if (pbi == null) {
            pbi = new PatientBasicInfo();
            pbi.setPatientId(patientId);
        }

        // ext1 用作 lifestyle（前端展示用）
        if (!lifestyle.isEmpty()) pbi.setExt1(lifestyle);

        // pastHistory / familyHistory 写入 ext5 JSON（兼容后续扩展）
        Map<String, String> ext5 = parsePatientExt5(pbi);
        if (!pastHistory.isEmpty()) ext5.put("pastHistory", pastHistory);
        if (!familyHistory.isEmpty()) ext5.put("familyHistory", familyHistory);
        pbi.setExt5(writeJsonSafe(ext5));

        if (patientBasicInfoMapper.findByPatientId(patientId) == null) {
            patientBasicInfoMapper.insert(pbi);
        } else {
            patientBasicInfoMapper.updateByPatientId(pbi);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "更新成功");
        result.put("data", true);
        return result;
    }

    private static String safeTrim(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s == null ? "" : s.trim();
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String writeJsonSafe(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception ignored) {
            return null;
        }
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false, name = "q") String q,
                                       @RequestParam(required = false) String riskLevel,
                                       @RequestParam(required = false) String disease,
                                       @RequestParam(required = false) String syndrome,
                                       @RequestParam(required = false) String responsibleDoctor) {
        // 兼容前端传 q 或 keyword
        if (keyword == null && q != null) {
            keyword = q;
        }
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        if (keyword != null) {
            if (keyword.length() > 30) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "keyword 过长（最多 30 位）");
                return result;
            }
            if (!KEYWORD_PATTERN.matcher(keyword).matches()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "keyword 仅支持中文/字母/数字");
                return result;
            }

            boolean allDigits = true;
            for (int i = 0; i < keyword.length(); i++) {
                if (!Character.isDigit(keyword.charAt(i))) {
                    allDigits = false;
                    break;
                }
            }
            if (allDigits && keyword.length() < 4) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "keyword 为纯数字时长度至少 4 位");
                return result;
            }
        }

        if (riskLevel != null && !riskLevel.trim().isEmpty()) {
            String rl = riskLevel.trim().toUpperCase();
            if (!ALLOWED_RISK_LEVELS.contains(rl)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "riskLevel 参数不合法，仅支持 HIGH/MID/LOW");
                return result;
            }
            riskLevel = rl;
        }

        if (disease != null) {
            disease = disease.trim();
            if (disease.isEmpty() || "全部".equals(disease)) {
                disease = null;
            }
        }
        if (syndrome != null) {
            syndrome = syndrome.trim();
            if (syndrome.isEmpty() || "全部".equals(syndrome) || "全部证型".equals(syndrome)) {
                syndrome = null;
            }
        }
        if (responsibleDoctor != null) {
            responsibleDoctor = responsibleDoctor.trim();
            if (responsibleDoctor.isEmpty() || "全部".equals(responsibleDoctor)) {
                responsibleDoctor = null;
            }
        }

        if (pageSize != null && (pageSize < 1 || pageSize > 200)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "pageSize 参数不合法，仅支持 1-200");
            return result;
        }

        // 兼容前端传 pageNo 或 page
        int p = pageNo != null ? Math.max(1, pageNo) : (page != null ? Math.max(1, page) : 1);
        int ps = pageSize == null ? 10 : pageSize;
        int offset = (p - 1) * ps;

        Long total = userMapper.countPatientSummary(keyword, riskLevel, disease, syndrome, responsibleDoctor);
        List<Map<String, Object>> rows = userMapper.selectPatientSummaryPage(keyword, riskLevel, disease, syndrome, responsibleDoctor, offset, ps);

        Map<String, Object> data = new HashMap<>();
        long totalVal = total == null ? 0L : total;
        long totalPages = ps <= 0 ? 1 : (long) Math.ceil(totalVal * 1.0 / ps);
        data.put("pageNo", p);
        data.put("pageSize", ps);
        data.put("total", totalVal);
        data.put("totalPages", totalPages);
        data.put("rows", rows == null ? Collections.emptyList() : rows);

        data.put("diseaseList", patientBasicInfoMapper.selectDistinctDiseases());
        data.put("syndromeList", patientBasicInfoMapper.selectDistinctSyndromes());

        List<Map<String, Object>> rawDoctors = userMapper.selectDoctorsForOrgUser();
        List<Map<String, Object>> doctorList = new ArrayList<>();
        if (rawDoctors != null) {
            for (Map<String, Object> r : rawDoctors) {
                if (r == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>(r);
                Object id = item.get("id");
                Object name = item.get("name");
                Object username = item.get("username");
                String display = null;
                if (name != null && !String.valueOf(name).trim().isEmpty()) {
                    display = String.valueOf(name).trim();
                } else if (username != null && !String.valueOf(username).trim().isEmpty()) {
                    display = String.valueOf(username).trim();
                }
                item.put("value", id);
                item.put("label", display == null ? "" : display);
                item.put("text", display == null ? "" : display);
                doctorList.add(item);
            }
        }
        data.put("doctorList", doctorList);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        Map<String, Object> result = new HashMap<>();
        if (id == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: id");
            return result;
        }

        User patient = userMapper.findPatientById(id);
        if (patient == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "患者不存在");
            return result;
        }

        PatientBasicInfo pbi = patientBasicInfoMapper.findByPatientId(id);
        Map<String, String> ext5 = parsePatientExt5(pbi);
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("id", patient.getId());
        basicInfo.put("name", patient.getName());
        basicInfo.put("gender", patient.getSex());
        basicInfo.put("age", patient.getAge());
        basicInfo.put("phone", patient.getPhone());
        basicInfo.put("address", patient.getAddress());
        basicInfo.put("mainDisease", pbi == null ? null : pbi.getExt3());
        basicInfo.put("syndrome", pbi == null ? null : pbi.getExt4());

        Map<String, Object> diagnosis = new HashMap<>();
        diagnosis.put("westernDiagnoses", new ArrayList<>());
        diagnosis.put("tcmSyndromes", new ArrayList<>());
        diagnosis.put("comorbidities", new ArrayList<>());

        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("medicalRecordNo", null);
        extraInfo.put("emergencyContact", null);
        extraInfo.put("insuranceType", null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("constitution", ext5.get("constitution"));
        profile.put("familyHistory", ext5.get("familyHistory"));
        profile.put("lifestyle", new ArrayList<>());
        profile.put("adherenceScore", null);

        Map<String, Object> riskSummary = new HashMap<>();
        riskSummary.put("riskLevel", patient.getRiskLevel());
        riskSummary.put("doctorName", userMapper.findPatientResponsibleDoctorName(id));
        riskSummary.put("lastFollowTime", userMapper.findPatientLastFollowTime(id));
        riskSummary.put("summary", "");

        Map<String, Object> data = new HashMap<>();
        data.put("basicInfo", basicInfo);
        data.put("diagnosis", diagnosis);
        data.put("extraInfo", extraInfo);
        data.put("profile", profile);
        data.put("riskSummary", riskSummary);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/recent-syndrome-worsen")
    public Map<String, Object> recentSyndromeWorsen(@RequestParam(required = false) Integer days,
                                                    @RequestParam(required = false) Integer limit) {
        int d = days == null ? 30 : Math.max(1, Math.min(days, 365));
        int l = limit == null ? 10 : Math.max(1, Math.min(limit, 50));

        Map<String, Object> serviceResult = dashboardService.syndromeAbnormalPatients(d, l);

        Map<String, Object> result = new HashMap<>();
        if (serviceResult == null) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常");
            return result;
        }

        Object dataObj = serviceResult.get("data");
        List<Map<String, Object>> rows = new ArrayList<>();
        if (dataObj instanceof List<?>) {
            for (Object o : (List<?>) dataObj) {
                if (!(o instanceof DashboardSyndromeAbnormalPatient)) {
                    continue;
                }
                DashboardSyndromeAbnormalPatient p = (DashboardSyndromeAbnormalPatient) o;
                Map<String, Object> row = new HashMap<>();
                row.put("patientId", p.getPatientId());
                row.put("patientName", p.getName());
                row.put("changeDesc", buildChangeDesc(p));
                row.put("timeText", buildTimeText(p.getAssessedAt()));
                rows.add(row);
            }
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", rows);
        return result;
    }

    private String buildChangeDesc(DashboardSyndromeAbnormalPatient p) {
        if (p == null) {
            return "";
        }
        String syndrome = p.getSyndromeName();
        Double score = p.getScore();
        if (syndrome == null) {
            syndrome = "";
        }
        if (score == null) {
            return syndrome.isEmpty() ? "证候加重" : "证候加重：" + syndrome;
        }
        String scoreText = String.format("%.2f", score);
        return syndrome.isEmpty() ? ("证候加重(评分=" + scoreText + ")") : ("证候加重：" + syndrome + "(评分=" + scoreText + ")");
    }

    private String buildTimeText(LocalDateTime at) {
        if (at == null) {
            return "";
        }
        return at.toString();
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


    private static String firstNonBlank(Object... values) {
        if (values == null) {
            return "";
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String s = String.valueOf(value).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return "";
    }

    private static String formatDateTime(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return "";
        }
        return s.replace('T', ' ');
    }

    private static String mapFollowupResultText(String value) {
        String s = firstNonBlank(value).trim();
        if (s.isEmpty()) {
            return "";
        }
        String u = s.toUpperCase();
        if (u.contains("COMPLETED") || u.contains("DONE") || u.contains("SUCCESS") || u.contains("FINISHED")) {
            return "已完成";
        }
        if (u.contains("FAILED") || u.contains("FAIL") || u.contains("ERROR")) {
            return "失败";
        }
        if (u.contains("CANCEL")) {
            return "已取消";
        }
        if (u.contains("PENDING") || u.contains("WAIT")) {
            return "待处理";
        }
        if (u.contains("PROCESS") || u.contains("FOLLOW")) {
            return "进行中";
        }
        return s;
    }

    private static String mapSeverityText(String severity) {
        String s = firstNonBlank(severity).trim();
        if (s.isEmpty()) {
            return "";
        }
        String u = s.toUpperCase();
        if (u.contains("RED") || u.contains("HIGH") || u.contains("3")) {
            return "高危";
        }
        if (u.contains("WARN") || u.contains("MID") || u.contains("MED") || u.contains("2")) {
            return "预警";
        }
        if (u.contains("LOW") || u.contains("GREEN") || u.contains("1") || u.contains("INFO")) {
            return "提示";
        }
        if (s.contains("高")) {
            return "高危";
        }
        if (s.contains("中") || s.contains("黄")) {
            return "预警";
        }
        if (s.contains("低") || s.contains("绿") || s.contains("提")) {
            return "提示";
        }
        return s;
    }

    private static String mapAlertStatusText(String status) {
        String s = firstNonBlank(status).trim();
        if (s.isEmpty()) {
            return "未处理";
        }
        String u = s.toUpperCase();
        if (u.equals("NEW")) {
            return "未处理";
        }
        if (u.contains("IN_PROGRESS") || u.contains("PROCESS") || u.contains("FOLLOWUP_CREATED")) {
            return "处理中";
        }
        if (u.contains("CLOSED") || u.contains("REVIEWED") || u.contains("IGNORED") || u.contains("DONE") || u.contains("COMPLETED")) {
            return "已关闭";
        }
        return s;
    }

    private static String mapAlertSourceText(String source, Object summaryObj) {
        String s = firstNonBlank(source).trim();
        String summary = firstNonBlank(summaryObj).trim().toUpperCase();
        String metric = summary;
        int idx = summary.indexOf(' ');
        if (idx > 0) {
            metric = summary.substring(0, idx).trim();
        }
        if ("DEVICE".equalsIgnoreCase(s)) {
            return "设备告警";
        }
        if (metric.equals("BP") || metric.contains("SBP") || metric.contains("DBP") || metric.contains("PRESS")) {
            return "血压告警";
        }
        if (metric.equals("HR") || metric.contains("HEART")) {
            return "心率告警";
        }
        if (metric.equals("SPO2") || metric.contains("O2")) {
            return "血氧告警";
        }
        if (metric.contains("TEMP")) {
            return "体温告警";
        }
        if (metric.contains("WEIGHT")) {
            return "体重告警";
        }
        if ("HEALTH".equalsIgnoreCase(s)) {
            return "健康告警";
        }
        return s.isEmpty() ? "健康告警" : s;
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
