package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.PatientBasicInfo;
import com.example.zhinengsuifang.entity.PatientDailyMeasurement;
import com.example.zhinengsuifang.entity.PatientMessage;
import com.example.zhinengsuifang.entity.PatientTcmSurvey;
import com.example.zhinengsuifang.entity.TCMDiagnosis;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.AlertCenterMapper;
import com.example.zhinengsuifang.mapper.PatientBasicInfoMapper;
import com.example.zhinengsuifang.mapper.PatientDailyMeasurementMapper;
import com.example.zhinengsuifang.mapper.PatientFollowupRecordMapper;
import com.example.zhinengsuifang.mapper.PatientMessageMapper;
import com.example.zhinengsuifang.mapper.PatientTcmSurveyMapper;
import com.example.zhinengsuifang.mapper.TCMDiagnosisMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "AI相关接口")
public class ApiAIController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
    private PatientMessageMapper patientMessageMapper;

    @Resource
    private AlertCenterMapper alertCenterMapper;

    @Resource
    private TCMDiagnosisMapper tcmDiagnosisMapper;

    @Value("${ai.openai.api-key:}")
    private String aiApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String aiBaseUrl;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String aiModel;

    @Value("${ai.openai.chat-path:/v1/chat/completions}")
    private String aiChatPath;

    @Value("${ai.openai.timeout-seconds:30}")
    private Integer aiTimeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        TCMDiagnosis latestTCM = tcmDiagnosisMapper.selectLatestByPatientId(patientId);

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

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (body == null) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "参数错误");
                return result;
            }

            Long authUserId = AuthHeaderUtil.getUserId(request);
            Long patientId = toLong(body.get("patientId"));
            if (patientId == null) {
                patientId = authUserId;
            }
            if (patientId == null) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "缺少 patientId");
                return result;
            }

            User patient = userMapper.findPatientById(patientId);
            if (patient == null) {
                result.put("success", false);
                result.put("code", ApiCode.NOT_FOUND.getCode());
                result.put("message", "患者不存在");
                return result;
            }

            if (!hasPermission(authUserId, patientId)) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "无权限访问其他患者会话");
                return result;
            }

            String message = safeText(body.get("message")).trim();
            if (message.isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "message 不能为空");
                return result;
            }

            PatientBasicInfo basicInfo = patientBasicInfoMapper.findByPatientId(patientId);
            PatientDailyMeasurement latestMeasurement = patientDailyMeasurementMapper.findLatest(patientId);
            List<PatientDailyMeasurement> recentMeasurements = patientDailyMeasurementMapper.findRecent(patientId, 7);
            List<PatientTcmSurvey> surveyRows = patientTcmSurveyMapper.findByPatientId(patientId);
            PatientTcmSurvey latestSurvey = (surveyRows == null || surveyRows.isEmpty()) ? null : surveyRows.get(0);
            List<Map<String, Object>> recentFollowups = patientFollowupRecordMapper.findRecentByPatientId(patientId, 3);
            PatientMessage latestMessage = patientMessageMapper.findLatestByPatientId(patientId);
            List<Map<String, Object>> recentAlerts = alertCenterMapper.selectAlertCenterPage(patientId, null, null, null, null,
                    null, null, 0, 3);
            TCMDiagnosis latestTCM = tcmDiagnosisMapper.selectLatestByPatientId(patientId);

            List<Map<String, String>> historyMessages = normalizeHistory(body.get("history"));
            Map<String, Object> patientContext = buildPatientContext(patient, basicInfo, latestMeasurement, recentMeasurements,
                    latestSurvey, recentFollowups, latestMessage, recentAlerts, latestTCM);
            String systemPrompt = buildChatSystemPrompt(patientContext);

            String bodyApiKey = safeText(body.get("apiKey")).trim();
            if (bodyApiKey.isEmpty()) {
                bodyApiKey = safeText(body.get("apikey")).trim();
            }
            String headerApiKey = request == null ? "" : safeText(request.getHeader("X-AI-API-KEY")).trim();
            String effectiveApiKey = !bodyApiKey.isEmpty() ? bodyApiKey : (!headerApiKey.isEmpty() ? headerApiKey : safeText(aiApiKey).trim());

            String reply;
            boolean fallback = false;
            if (effectiveApiKey.isEmpty()) {
                fallback = true;
                reply = buildFallbackReply(message, patientContext);
            } else {
                try {
                    reply = callOpenAiChat(systemPrompt, historyMessages, message, effectiveApiKey);
                    if (reply == null || reply.trim().isEmpty()) {
                        throw new IllegalStateException("AI 返回为空");
                    }
                } catch (Exception ex) {
                    fallback = true;
                    reply = buildFallbackReply(message, patientContext);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("patientId", patientId);
            data.put("reply", reply);
            data.put("content", reply);
            data.put("fallback", fallback);
            data.put("aiScore", calculateAIScore(patient, latestTCM));
            data.put("aiFocus", generateAIFocus(patient, latestTCM));
            data.put("contextSummary", patientContext.get("contextSummary"));
            data.put("usedContext", patientContext.get("usedContext"));
            data.put("latestMeasureBrief", patientContext.get("latestMeasureBrief"));

            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", fallback ? "AI 服务暂不可用，已返回保守建议" : "回复成功");
            result.put("data", data);
            return result;
        } catch (Exception ex) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + safeText(ex.getMessage()));
            return result;
        }
    }

    private boolean hasPermission(Long authUserId, Long patientId) {
        if (authUserId == null || patientId == null) {
            return true;
        }
        if (authUserId.equals(patientId)) {
            return true;
        }
        User operator = userMapper.findById(authUserId);
        String role = operator == null ? "" : safeText(operator.getRole()).trim().toUpperCase(Locale.ROOT);
        return "DOCTOR".equals(role) || "FOLLOW_UP".equals(role);
    }

    private Map<String, Object> buildPatientContext(User patient,
                                                    PatientBasicInfo basicInfo,
                                                    PatientDailyMeasurement latestMeasurement,
                                                    List<PatientDailyMeasurement> recentMeasurements,
                                                    PatientTcmSurvey latestSurvey,
                                                    List<Map<String, Object>> recentFollowups,
                                                    PatientMessage latestMessage,
                                                    List<Map<String, Object>> recentAlerts,
                                                    TCMDiagnosis latestTCM) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> usedContext = new HashMap<>();

        Map<String, String> ext5 = parsePatientExt5(basicInfo);
        String disease = basicInfo == null ? "" : safeText(basicInfo.getExt3());
        String syndrome = basicInfo == null ? "" : safeText(basicInfo.getExt4());
        String constitution = ext5.getOrDefault("constitution", "");
        String familyHistory = ext5.getOrDefault("familyHistory", "");
        String pastHistory = ext5.getOrDefault("pastHistory", "");

        String latestMeasureBrief = buildLatestMeasurementBrief(latestMeasurement);
        String trendSummary = buildMeasurementTrendSummary(recentMeasurements);
        String surveySummary = buildSurveySummary(latestSurvey);
        String followupSummary = buildFollowupSummary(recentFollowups);
        String latestAdviceSummary = buildLatestAdviceSummary(latestMessage, recentFollowups);
        String alertSummary = buildAlertSummary(recentAlerts);
        String tcmSummary = buildTcmSummary(latestTCM);

        usedContext.put("profile", true);
        usedContext.put("measurement", latestMeasurement != null || (recentMeasurements != null && !recentMeasurements.isEmpty()));
        usedContext.put("survey", latestSurvey != null);
        usedContext.put("followup", recentFollowups != null && !recentFollowups.isEmpty());
        usedContext.put("advice", !latestAdviceSummary.isEmpty());
        usedContext.put("alerts", recentAlerts != null && !recentAlerts.isEmpty());
        usedContext.put("tcm", latestTCM != null);

        StringBuilder summary = new StringBuilder();
        summary.append("【患者档案】\n");
        summary.append("姓名：").append(safeText(patient.getName()).isEmpty() ? "患者" : safeText(patient.getName())).append("\n");
        summary.append("年龄：").append(patient.getAge() == null ? "未知" : patient.getAge()).append("\n");
        summary.append("性别：").append(safeText(patient.getSex()).isEmpty() ? "未知" : safeText(patient.getSex())).append("\n");
        summary.append("风险等级：").append(safeText(patient.getRiskLevel()).isEmpty() ? "未知" : safeText(patient.getRiskLevel())).append("\n");
        if (!disease.isEmpty()) {
            summary.append("主要慢病：").append(disease).append("\n");
        }
        if (!syndrome.isEmpty()) {
            summary.append("证型/证候：").append(syndrome).append("\n");
        }
        if (!constitution.isEmpty()) {
            summary.append("体质：").append(constitution).append("\n");
        }
        if (!pastHistory.isEmpty()) {
            summary.append("既往史：").append(pastHistory).append("\n");
        }
        if (!familyHistory.isEmpty()) {
            summary.append("家族史：").append(familyHistory).append("\n");
        }
        summary.append("AI 风险分：").append(calculateAIScore(patient, latestTCM)).append("\n");
        summary.append("AI 关注点：").append(generateAIFocus(patient, latestTCM)).append("\n\n");

        summary.append("【近期体征】\n");
        summary.append(latestMeasureBrief.isEmpty() ? "暂无最近监测数据\n" : latestMeasureBrief + "\n");
        summary.append(trendSummary.isEmpty() ? "暂无趋势摘要\n\n" : trendSummary + "\n\n");

        summary.append("【近期问卷/症状】\n");
        summary.append(surveySummary.isEmpty() ? "暂无近期问卷结果\n\n" : surveySummary + "\n\n");

        summary.append("【最近随访与医嘱】\n");
        summary.append(followupSummary.isEmpty() ? "暂无近期随访记录\n" : followupSummary + "\n");
        summary.append(latestAdviceSummary.isEmpty() ? "暂无最近建议消息\n\n" : latestAdviceSummary + "\n\n");

        summary.append("【近期预警】\n");
        summary.append(alertSummary.isEmpty() ? "暂无近期预警\n\n" : alertSummary + "\n\n");

        summary.append("【中医四诊】\n");
        summary.append(tcmSummary.isEmpty() ? "暂无近期中医四诊\n" : tcmSummary + "\n");

        context.put("contextSummary", summary.toString());
        context.put("usedContext", usedContext);
        context.put("latestMeasureBrief", latestMeasureBrief);
        context.put("trendSummary", trendSummary);
        context.put("surveySummary", surveySummary);
        context.put("followupSummary", followupSummary);
        context.put("latestAdviceSummary", latestAdviceSummary);
        context.put("alertSummary", alertSummary);
        context.put("tcmSummary", tcmSummary);
        return context;
    }

    private String buildChatSystemPrompt(Map<String, Object> patientContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是\"寒岐智护\"慢性病随访健康助手。\n");
        sb.append("请严格根据提供的患者上下文回答。\n");
        sb.append("回答要求：\n");
        sb.append("1. 仅用中文回答，语言通俗、稳健、可执行。\n");
        sb.append("2. 优先结合患者最近体征、问卷、随访、预警和中医信息进行解释。\n");
        sb.append("3. 如果某类数据缺失，要明确说“当前系统里没有这部分最新数据”，不要编造。\n");
        sb.append("4. 不要替代医生下诊断，不要编造化验/影像结果。\n");
        sb.append("5. 如果出现持续极高血压、胸痛、呼吸困难、晕厥、意识异常、血氧明显下降等危险信号，必须明确建议及时就医。\n");
        sb.append("6. 回答格式尽量包含：当前情况判断、与近期数据的关系、可执行建议、何时联系医生/就医。\n\n");
        sb.append(safeText(patientContext.get("contextSummary")));
        sb.append("\n请基于以上上下文回答用户当前问题。\n");
        return sb.toString();
    }

    private String callOpenAiChat(String systemPrompt,
                                  List<Map<String, String>> historyMessages,
                                  String userMessage,
                                  String apiKey) throws Exception {
        String base = safeText(aiBaseUrl).trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String path = safeText(aiChatPath).trim();
        if (path.isEmpty()) {
            path = "/v1/chat/completions";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        URI uri = URI.create(base + path);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);

        if (historyMessages != null && !historyMessages.isEmpty()) {
            messages.addAll(historyMessages);
        }

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        Map<String, Object> req = new HashMap<>();
        req.put("model", aiModel);
        req.put("temperature", 0.35);
        req.put("messages", messages);

        String json = objectMapper.writeValueAsString(req);
        int timeout = aiTimeoutSeconds == null ? 30 : Math.max(3, aiTimeoutSeconds);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeout, 30)))
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI 请求失败，HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            return null;
        }
        return content.asText();
    }

    private String buildFallbackReply(String userMessage, Map<String, Object> patientContext) {
        String question = safeText(userMessage).trim();
        String trendSummary = safeText(patientContext.get("trendSummary"));
        String latestMeasureBrief = safeText(patientContext.get("latestMeasureBrief"));
        String surveySummary = safeText(patientContext.get("surveySummary"));
        String followupSummary = safeText(patientContext.get("followupSummary"));
        String alertSummary = safeText(patientContext.get("alertSummary"));
        String tcmSummary = safeText(patientContext.get("tcmSummary"));

        String lower = question.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        sb.append("结合您最近的身体状况，先给您一个保守建议：\n");

        if (lower.contains("血压") || question.contains("头晕") || question.contains("头痛")) {
            sb.append("1. 当前问题与血压波动高度相关，建议先安静休息 5-10 分钟后复测血压。\n");
            if (!latestMeasureBrief.isEmpty()) {
                sb.append("2. 最近一次体征：").append(latestMeasureBrief).append("\n");
            }
            if (!trendSummary.isEmpty()) {
                sb.append("3. 近期趋势：").append(trendSummary).append("\n");
            }
            sb.append("4. 请继续规律用药、避免熬夜、少盐饮食，今天尽量不要情绪激动或剧烈运动。\n");
            sb.append("5. 如果反复明显升高，或伴胸痛、胸闷、视物模糊、肢体乏力，请尽快就医。\n");
        } else if (lower.contains("血糖") || question.contains("口渴") || question.contains("乏力")) {
            sb.append("1. 先注意规律进食、规律用药，必要时复测血糖。\n");
            if (!latestMeasureBrief.isEmpty()) {
                sb.append("2. 最近一次体征：").append(latestMeasureBrief).append("\n");
            }
            if (!surveySummary.isEmpty()) {
                sb.append("3. 最近问卷/症状：").append(surveySummary).append("\n");
            }
            sb.append("4. 如出现明显心慌、手抖、出冷汗或持续不适，请及时联系医生。\n");
        } else {
            if (!latestMeasureBrief.isEmpty()) {
                sb.append("1. 最近一次体征：").append(latestMeasureBrief).append("\n");
            } else {
                sb.append("1. 当前系统里缺少足够新的体征数据，建议先补充一次测量后我再结合数据判断。\n");
            }
            if (!trendSummary.isEmpty()) {
                sb.append("2. 近期趋势：").append(trendSummary).append("\n");
            }
            if (!surveySummary.isEmpty()) {
                sb.append("3. 最近问卷/症状：").append(surveySummary).append("\n");
            }
            if (!followupSummary.isEmpty()) {
                sb.append("4. 最近随访：").append(followupSummary).append("\n");
            }
            if (!tcmSummary.isEmpty()) {
                sb.append("5. 中医四诊摘要：").append(tcmSummary).append("\n");
            }
            sb.append("6. 建议先保持规律作息、清淡饮食、按时用药，若症状持续加重，请联系医生或复诊。\n");
        }

        if (!alertSummary.isEmpty()) {
            sb.append("\n系统近期预警提示：").append(alertSummary).append("\n");
        }
        sb.append("\n说明：当前回复为系统保守建议，仅供慢病随访管理参考，不能替代医生面诊。");
        return sb.toString();
    }

    private List<Map<String, String>> normalizeHistory(Object historyObj) {
        List<Map<String, String>> result = new ArrayList<>();
        if (!(historyObj instanceof List<?> rows)) {
            return result;
        }
        int start = Math.max(0, rows.size() - 12);
        for (int i = start; i < rows.size(); i++) {
            Object item = rows.get(i);
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String role = safeText(map.get("role")).trim().toLowerCase(Locale.ROOT);
            String content = safeText(map.get("content")).trim();
            if (content.isEmpty()) {
                continue;
            }
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            Map<String, String> msg = new HashMap<>();
            msg.put("role", role);
            msg.put("content", content);
            result.add(msg);
        }
        return result;
    }

    private String buildLatestMeasurementBrief(PatientDailyMeasurement measurement) {
        if (measurement == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (measurement.getMeasuredAt() != null) {
            parts.add("时间 " + formatDateTime(measurement.getMeasuredAt()));
        }
        if (measurement.getSbp() != null || measurement.getDbp() != null) {
            parts.add("血压 " + valueOrDash(measurement.getSbp()) + "/" + valueOrDash(measurement.getDbp()) + " mmHg");
        }
        if (measurement.getHeartRate() != null) {
            parts.add("心率 " + formatDouble(measurement.getHeartRate()) + " 次/分");
        }
        if (measurement.getSpo2() != null) {
            parts.add("血氧 " + formatDouble(measurement.getSpo2()) + "%");
        }
        if (measurement.getGlucose() != null) {
            parts.add("血糖 " + formatDouble(measurement.getGlucose()) + " mmol/L");
        }
        if (measurement.getTemperatureC() != null) {
            parts.add("体温 " + formatDouble(measurement.getTemperatureC()) + "℃");
        }
        if (measurement.getSleep() != null) {
            parts.add("睡眠 " + formatDouble(measurement.getSleep()) + " 小时");
        }
        if (!safeText(measurement.getSymptoms()).isEmpty()) {
            parts.add("症状 " + safeText(measurement.getSymptoms()));
        }
        return String.join("；", parts);
    }

    private String buildMeasurementTrendSummary(List<PatientDailyMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            return "";
        }
        List<PatientDailyMeasurement> rows = new ArrayList<>();
        for (PatientDailyMeasurement row : measurements) {
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            return "";
        }
        rows.sort(Comparator.comparing(PatientDailyMeasurement::getMeasuredAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Double avgSbp = avg(rows, "sbp");
        Double avgDbp = avg(rows, "dbp");
        Double avgHr = avg(rows, "heartRate");
        Double avgSpo2 = avg(rows, "spo2");
        Double avgSleep = avg(rows, "sleep");

        PatientDailyMeasurement latest = rows.get(0);
        PatientDailyMeasurement oldest = rows.get(rows.size() - 1);

        List<String> parts = new ArrayList<>();
        parts.add("近 " + rows.size() + " 次监测");
        if (avgSbp != null || avgDbp != null) {
            parts.add("平均血压 " + valueOrDash(avgSbp) + "/" + valueOrDash(avgDbp) + " mmHg");
        }
        if (avgHr != null) {
            parts.add("平均心率 " + formatDouble(avgHr) + " 次/分");
        }
        if (avgSpo2 != null) {
            parts.add("平均血氧 " + formatDouble(avgSpo2) + "%");
        }
        if (avgSleep != null) {
            parts.add("平均睡眠 " + formatDouble(avgSleep) + " 小时");
        }

        String bpTrend = buildTrend(latest == null ? null : latest.getSbp(), oldest == null ? null : oldest.getSbp(), 5D, "收缩压");
        if (!bpTrend.isEmpty()) {
            parts.add(bpTrend);
        }
        String hrTrend = buildTrend(latest == null ? null : latest.getHeartRate(), oldest == null ? null : oldest.getHeartRate(), 5D, "心率");
        if (!hrTrend.isEmpty()) {
            parts.add(hrTrend);
        }

        List<String> abnormal = new ArrayList<>();
        if (latest != null) {
            if ((latest.getSbp() != null && latest.getSbp() >= 140) || (latest.getDbp() != null && latest.getDbp() >= 90)) {
                abnormal.add("最近血压偏高");
            }
            if (latest.getHeartRate() != null && latest.getHeartRate() > 100) {
                abnormal.add("最近心率偏快");
            }
            if (latest.getSpo2() != null && latest.getSpo2() < 95) {
                abnormal.add("最近血氧偏低");
            }
            if (latest.getTemperatureC() != null && latest.getTemperatureC() >= 37.3) {
                abnormal.add("最近体温偏高");
            }
        }
        if (!abnormal.isEmpty()) {
            parts.add("异常提示：" + String.join("、", abnormal));
        }
        return String.join("；", parts);
    }

    private String buildSurveySummary(PatientTcmSurvey survey) {
        if (survey == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (survey.getAssessedAt() != null) {
            parts.add("时间 " + formatDateTime(survey.getAssessedAt()));
        }
        if (!safeText(survey.getSurveyType()).isEmpty()) {
            parts.add("问卷类型 " + safeText(survey.getSurveyType()));
        }
        String resultBrief = shortenJsonOrText(survey.getResultJson(), 220);
        if (!resultBrief.isEmpty()) {
            parts.add("结果摘要 " + resultBrief);
        } else {
            String answerBrief = shortenJsonOrText(survey.getAnswersJson(), 180);
            if (!answerBrief.isEmpty()) {
                parts.add("填写内容摘要 " + answerBrief);
            }
        }
        return String.join("；", parts);
    }

    private String buildFollowupSummary(List<Map<String, Object>> followups) {
        if (followups == null || followups.isEmpty()) {
            return "";
        }
        Map<String, Object> latest = followups.get(0);
        if (latest == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (!isBlank(latest.get("followupTime"))) {
            parts.add("时间 " + String.valueOf(latest.get("followupTime")));
        }
        if (!isBlank(latest.get("followupType"))) {
            parts.add("类型 " + String.valueOf(latest.get("followupType")));
        }
        if (!isBlank(latest.get("riskLevel"))) {
            parts.add("风险等级 " + String.valueOf(latest.get("riskLevel")));
        }
        if (!isBlank(latest.get("contentSummary"))) {
            parts.add("随访结论 " + String.valueOf(latest.get("contentSummary")));
        }
        if (!isBlank(latest.get("nextPlanTime"))) {
            parts.add("下次计划 " + String.valueOf(latest.get("nextPlanTime")));
        }
        return String.join("；", parts);
    }

    private String buildLatestAdviceSummary(PatientMessage latestMessage, List<Map<String, Object>> followups) {
        if (latestMessage != null) {
            List<String> parts = new ArrayList<>();
            if (latestMessage.getCreatedAt() != null) {
                parts.add("时间 " + formatDateTime(latestMessage.getCreatedAt()));
            }
            if (!safeText(latestMessage.getTitle()).isEmpty()) {
                parts.add("标题 " + safeText(latestMessage.getTitle()));
            }
            if (!safeText(latestMessage.getContent()).isEmpty()) {
                parts.add("内容 " + safeText(latestMessage.getContent()));
            }
            return String.join("；", parts);
        }
        if (followups != null && !followups.isEmpty()) {
            Map<String, Object> latest = followups.get(0);
            if (latest != null && !isBlank(latest.get("contentSummary"))) {
                return "最近随访建议：" + String.valueOf(latest.get("contentSummary"));
            }
        }
        return "";
    }

    private String buildAlertSummary(List<Map<String, Object>> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Map<String, Object> alert : alerts) {
            if (alert == null) {
                continue;
            }
            List<String> item = new ArrayList<>();
            if (!isBlank(alert.get("alertTime"))) {
                item.add(String.valueOf(alert.get("alertTime")));
            }
            if (!isBlank(alert.get("level"))) {
                item.add("级别 " + String.valueOf(alert.get("level")));
            }
            if (!isBlank(alert.get("summary"))) {
                item.add(String.valueOf(alert.get("summary")));
            }
            if (!item.isEmpty()) {
                parts.add(String.join("，", item));
            }
        }
        return parts.isEmpty() ? "" : String.join("；", parts);
    }

    private String buildTcmSummary(TCMDiagnosis tcm) {
        if (tcm == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (tcm.getDiagnosisDate() != null) {
            parts.add("时间 " + formatDateTime(tcm.getDiagnosisDate()));
        }
        if (!safeText(tcm.getMainComplaint()).isEmpty()) {
            parts.add("主诉 " + safeText(tcm.getMainComplaint()));
        }
        if (!safeText(tcm.getTongueDescription()).isEmpty()) {
            parts.add("舌象 " + safeText(tcm.getTongueDescription()));
        }
        if (!safeText(tcm.getPulseDescription()).isEmpty()) {
            parts.add("脉象 " + safeText(tcm.getPulseDescription()));
        }
        if (!safeText(tcm.getTcmSummary()).isEmpty()) {
            parts.add("辨证摘要 " + safeText(tcm.getTcmSummary()));
        }
        return String.join("；", parts);
    }

    private Double avg(List<PatientDailyMeasurement> rows, String field) {
        double sum = 0D;
        int count = 0;
        for (PatientDailyMeasurement row : rows) {
            if (row == null) {
                continue;
            }
            Double value;
            switch (field) {
                case "sbp" -> value = row.getSbp();
                case "dbp" -> value = row.getDbp();
                case "heartRate" -> value = row.getHeartRate();
                case "spo2" -> value = row.getSpo2();
                case "sleep" -> value = row.getSleep();
                default -> value = null;
            }
            if (value != null) {
                sum += value;
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return sum / count;
    }

    private String buildTrend(Double latest, Double oldest, Double threshold, String label) {
        if (latest == null || oldest == null) {
            return "";
        }
        double delta = latest - oldest;
        double abs = Math.abs(delta);
        if (abs < (threshold == null ? 3D : threshold)) {
            return label + "整体平稳";
        }
        return label + (delta > 0 ? "较前升高" : "较前下降") + "约 " + formatDouble(abs);
    }

    private String shortenJsonOrText(String raw, int maxLen) {
        String text = safeText(raw).trim();
        if (text.isEmpty()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(text);
            String compact = node.toString();
            compact = compact.replace("\"", "");
            compact = compact.replace("{", "").replace("}", "").replace("[", "").replace("]", "");
            compact = compact.replace(":", "=").replace(",", "；");
            return crop(compact, maxLen);
        } catch (Exception ignored) {
            return crop(text, maxLen);
        }
    }

    private String crop(String raw, int maxLen) {
        String text = safeText(raw).replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private Map<String, String> parsePatientExt5(PatientBasicInfo basicInfo) {
        Map<String, String> out = new HashMap<>();
        out.put("constitution", "");
        out.put("familyHistory", "");
        out.put("pastHistory", "");
        if (basicInfo == null || isBlank(basicInfo.getExt5())) {
            return out;
        }
        try {
            Object obj = objectMapper.readValue(basicInfo.getExt5(), Object.class);
            if (obj instanceof Map<?, ?> map) {
                Object constitution = map.get("constitution");
                Object familyHistory = map.get("familyHistory");
                Object pastHistory = map.get("pastHistory");
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

    private Long toLong(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            String s = String.valueOf(raw).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Long.parseLong(s);
        } catch (Exception ex) {
            return null;
        }
    }

    private String safeText(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    private boolean isBlank(Object raw) {
        return safeText(raw).trim().isEmpty();
    }

    private String valueOrDash(Number n) {
        return n == null ? "-" : formatDouble(n.doubleValue());
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "-";
        }
        double v = value;
        if (Math.abs(v - Math.rint(v)) < 0.000001D) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.US, "%.1f", v);
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : DATE_TIME_FORMATTER.format(time);
    }

    private String calculateAIScore(User patient, TCMDiagnosis tcm) {
        int score = 0;

        if (patient.getAge() != null) {
            if (patient.getAge() > 60) score += 20;
            else if (patient.getAge() > 40) score += 10;
        }

        if (patient.getRiskLevel() != null) {
            String risk = patient.getRiskLevel().toUpperCase(Locale.ROOT);
            if (risk.contains("HIGH") || risk.contains("高")) score += 30;
            else if (risk.contains("MID") || risk.contains("MED") || risk.contains("中")) score += 15;
        }

        if (tcm != null) {
            if (tcm.getTcmSummary() != null && tcm.getTcmSummary().contains("虚")) score += 10;
            if (tcm.getPulseDescription() != null && tcm.getPulseDescription().contains("弱")) score += 10;
        }

        score = Math.min(100, Math.max(0, score));
        return String.valueOf(score);
    }

    private String generateAIFocus(User patient, TCMDiagnosis tcm) {
        StringBuilder focus = new StringBuilder();

        if (patient.getAge() != null && patient.getAge() > 60) {
            focus.append("年龄较大，");
        }

        if (patient.getRiskLevel() != null) {
            String risk = patient.getRiskLevel().toUpperCase(Locale.ROOT);
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
        }
        return "暂无特殊关注";
    }

    private String generateAIAdvice(User patient, TCMDiagnosis tcm) {
        StringBuilder advice = new StringBuilder();

        advice.append("1. 保持良好的生活习惯，规律作息，避免熬夜。\n");
        advice.append("2. 饮食宜清淡，避免辛辣刺激性食物。\n");
        advice.append("3. 适当进行有氧运动，如散步、太极拳等。\n");

        if (tcm != null && tcm.getTcmSummary() != null) {
            if (tcm.getTcmSummary().contains("虚")) {
                advice.append("4. 可适当食用补气养血的食物，如红枣、桂圆等。\n");
            }
            if (tcm.getTcmSummary().contains("湿")) {
                advice.append("4. 可食用祛湿的食物，如薏米、红豆等。\n");
            }
        }

        advice.append("5. 定期监测健康指标，如有不适及时就医。");
        return advice.toString();
    }
}
