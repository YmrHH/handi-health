package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.UpdateVisitPlanStatusRequest;
import com.example.zhinengsuifang.entity.DoctorAdvice;
import com.example.zhinengsuifang.entity.FollowUpSchedule;
import com.example.zhinengsuifang.entity.FollowUpTask;
import com.example.zhinengsuifang.entity.InterventionRecommend;
import com.example.zhinengsuifang.entity.PatientMessage;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.AlertCenterMapper;
import com.example.zhinengsuifang.mapper.FollowUpScheduleMapper;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.InterventionRecommendMapper;
import com.example.zhinengsuifang.mapper.PatientMessageMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.mapper.VisitPlanMapper;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import com.example.zhinengsuifang.service.DoctorAdviceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intervention")
@Tag(name = "复查计划", description = "复查/干预计划相关接口")
public class ApiInterventionController {

    @Resource
    private VisitPlanMapper visitPlanMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private FollowUpScheduleMapper followUpScheduleMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private InterventionRecommendMapper interventionRecommendMapper;

    @Resource
    private PatientMessageMapper patientMessageMapper;

    @Resource
    private AlertCenterMapper alertCenterMapper;

    @Resource
    private DoctorAdviceService doctorAdviceService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    @GetMapping("/recommend")
    public Map<String, Object> recommend(@RequestParam(required = false) String bodyType,
                                         @RequestParam(required = false) String pattern,
                                         @RequestParam(required = false) String disease,
                                         @RequestParam(required = false) String season) {
        Map<String, Object> data = new HashMap<>();
        data.put("bodyTypes", new ArrayList<>());
        data.put("patterns", new ArrayList<>());
        data.put("diseases", new ArrayList<>());
        data.put("bodyTypeParam", bodyType);
        data.put("patternParam", pattern);
        data.put("diseaseParam", disease);
        data.put("seasonParam", season);
        data.put("recommendList", new ArrayList<>());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private String buildAiPrompt(String patientName,
                                String riskLevel,
                                String triggerReason,
                                String bodyType,
                                String pattern,
                                String disease) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是三甲医院心血管科与中医结合随访系统的医生助手。\n");
        sb.append("请基于以下信息，生成一段可直接下发给患者的【个体化健康建议】。\n");
        sb.append("要求：\n");
        sb.append("1) 用中文；\n");
        sb.append("2) 语气专业、清晰、可执行；\n");
        sb.append("3) 分点输出，包含：风险提示、异常原因解释、生活方式建议、用药/复诊提醒（如适用）；\n");
        sb.append("4) 不要编造不存在的化验/检查结果；\n");
        sb.append("5) 不要输出任何隐私或系统内部字段名。\n\n");

        sb.append("患者姓名：").append(patientName).append("\n");
        if (riskLevel != null && !riskLevel.trim().isEmpty()) {
            sb.append("风险等级：").append(riskLevel).append("\n");
        }
        if (disease != null && !disease.trim().isEmpty()) {
            sb.append("病种：").append(disease).append("\n");
        }
        if (triggerReason != null && !triggerReason.trim().isEmpty()) {
            sb.append("触发原因：").append(triggerReason).append("\n");
        }
        if (bodyType != null && !bodyType.trim().isEmpty()) {
            sb.append("体质：").append(bodyType).append("\n");
        }
        if (pattern != null && !pattern.trim().isEmpty()) {
            sb.append("证型：").append(pattern).append("\n");
        }

        sb.append("\n输出格式示例：\n");
        sb.append("尊敬的XXX：\n- 【风险提示】...\n- 【原因与解读】...\n- 【建议】1)... 2)...\n- 【复诊/就医提醒】...\n");
        return sb.toString();
    }

    private String buildTemplateSuggestion(String patientName,
                                          String riskLevel,
                                          String triggerReason,
                                          String bodyType,
                                          String pattern) {
        StringBuilder suggestion = new StringBuilder();
        suggestion.append("尊敬的").append(patientName).append("：\n\n");
        suggestion.append("根据您的健康监测数据分析，为您提供以下个性化健康建议：\n\n");

        if (riskLevel != null && !riskLevel.trim().isEmpty()) {
            suggestion.append("【风险评估】您当前的风险等级为").append(riskLevel).append("，");
            if ("HIGH".equalsIgnoreCase(riskLevel)) {
                suggestion.append("建议密切关注健康指标变化，必要时及时就医。\n\n");
            } else {
                suggestion.append("请继续保持良好的生活习惯。\n\n");
            }
        }

        if (triggerReason != null && !triggerReason.trim().isEmpty()) {
            suggestion.append("【异常提醒】").append(triggerReason).append("\n");
            suggestion.append("建议：定期监测相关指标，如有不适请及时就医。\n\n");
        }

        if ((bodyType != null && !bodyType.trim().isEmpty()) || (pattern != null && !pattern.trim().isEmpty())) {
            suggestion.append("【中医调理】");
            if (bodyType != null && !bodyType.trim().isEmpty()) {
                suggestion.append("体质类型：").append(bodyType).append("；");
            }
            if (pattern != null && !pattern.trim().isEmpty()) {
                suggestion.append("证型：").append(pattern);
            }
            suggestion.append("\n建议：注意饮食调理，保持作息规律，适当运动。\n\n");
        }

        suggestion.append("【生活建议】\n");
        suggestion.append("1. 保持规律作息，保证充足睡眠\n");
        suggestion.append("2. 清淡饮食，少盐少油\n");
        suggestion.append("3. 适度运动，每周至少150分钟中等强度活动\n");
        suggestion.append("4. 定期监测血压、血糖等指标\n");
        suggestion.append("5. 按时服药，定期复诊\n\n");
        suggestion.append("如有任何不适，请及时联系医生。祝您身体健康！");
        return suggestion.toString();
    }

    private String callOpenAiChat(String prompt, String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("AI API Key 未配置");
        }

        String base = aiBaseUrl == null ? "" : aiBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String path = aiChatPath == null ? "" : aiChatPath.trim();
        if (path.isEmpty()) {
            path = "/v1/chat/completions";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        URI uri = URI.create(base + path);

        Map<String, Object> req = new HashMap<>();
        req.put("model", aiModel);
        req.put("temperature", 0.7);
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sys = new HashMap<>();
        sys.put("role", "system");
        sys.put("content", "你是医疗随访系统的医生助手，只输出建议文本。");
        messages.add(sys);
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", prompt);
        messages.add(user);
        req.put("messages", messages);

        String json = objectMapper.writeValueAsString(req);

        int timeout = aiTimeoutSeconds == null ? 30 : Math.max(1, aiTimeoutSeconds);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeout, 30)))
                .build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("AI 请求失败，HTTP " + resp.statusCode());
        }

        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            return null;
        }
        return content.asText();
    }

    @GetMapping("/home-service")
    public Map<String, Object> homeService(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        Long userId = AuthHeaderUtil.getUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }

        User doctor = userMapper.findById(userId);
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        List<Map<String, Object>> rows = followUpTaskMapper.selectHomeServiceTasksByDoctor(doctor.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                if (r == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>(r);
                Object statusObj = item.get("status");
                String status = statusObj == null ? "" : String.valueOf(statusObj);
                item.put("statusText", statusText(status));
                if (!item.containsKey("remark") || item.get("remark") == null) {
                    item.put("remark", "");
                }
                list.add(item);
            }
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @PostMapping("/home-service/assign")
    public Map<String, Object> homeServiceAssign(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Long taskId = toLong(body.get("taskId"));
        Long staffId = toLong(body.get("staffId"));
        if (taskId == null || staffId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "taskId 或 staffId 不能为空");
            return result;
        }

        int updated = followUpTaskMapper.updateFollowUpUser(taskId, staffId);
        if (updated <= 0) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "指派失败");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "指派成功");
        result.put("data", new HashMap<>());
        return result;
    }

    @GetMapping("/visit-plan")
    public Map<String, Object> visitPlan(HttpServletRequest request,
                                         @RequestParam(required = false) String doctorUsername,
                                         @RequestParam(required = false) String doctorPassword,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer pageSize,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) LocalDateTime startDate,
                                         @RequestParam(required = false) LocalDateTime endDate,
                                         @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();

        // 1) 优先 Header 鉴权（Bearer <userId>）
        User doctor = null;
        Long userId = AuthHeaderUtil.getUserId(request);
        if (userId != null) {
            User u = userMapper.findById(userId);
            if (u != null && u.getRole() != null && "DOCTOR".equalsIgnoreCase(u.getRole().trim())) {
                doctor = u;
            }
        }

        // 2) 回退到 doctorUsername/doctorPassword（兼容旧调用）
        if (doctor == null) {
            if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "医生账号或密码不能为空");
                return result;
            }

            doctor = userMapper.findByUsername(doctorUsername.trim());
            if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "無权限");
                return result;
            }

            boolean match = passwordEncoder.matches(doctorPassword, doctor.getPassword());
            if (!match) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "医生账号或密码错誤");
                return result;
            }
        }

        int p = page == null ? 1 : Math.max(1, page);
        int ps = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 200));
        int offset = (p - 1) * ps;

        Long total = visitPlanMapper.countPlans(status, startDate, endDate, keyword);
        List<Map<String, Object>> plans = visitPlanMapper.selectPlans(status, startDate, endDate, keyword, offset, ps);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total == null ? 0L : total);
        data.put("plans", plans == null ? Collections.emptyList() : plans);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/visit-plan/update-status")
    public Map<String, Object> updateVisitPlanStatus(HttpServletRequest request, @RequestBody UpdateVisitPlanStatusRequest body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        String doctorUsername = body.getDoctorUsername();
        String doctorPassword = body.getDoctorPassword();
        Long planId = body.getPlanId();
        String newStatus = body.getNewStatus();

        if (planId == null || newStatus == null || newStatus.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        // 1) 优先 Header 鉴权（Bearer <userId>）
        User doctor = null;
        Long userId = AuthHeaderUtil.getUserId(request);
        if (userId != null) {
            User u = userMapper.findById(userId);
            if (u != null && u.getRole() != null && "DOCTOR".equalsIgnoreCase(u.getRole().trim())) {
                doctor = u;
            }
        }

        // 2) 回退到 doctorUsername/doctorPassword（兼容旧调用）
        if (doctor == null) {
            if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "参数错誤");
                return result;
            }

            doctor = userMapper.findByUsername(doctorUsername.trim());
            if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "無权限");
                return result;
            }

            boolean match = passwordEncoder.matches(doctorPassword, doctor.getPassword());
            if (!match) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "医生账号或密码错誤");
                return result;
            }
        }

        int updated = visitPlanMapper.updateStatus(planId, newStatus.trim().toUpperCase());

        result.put("success", updated > 0);
        result.put("code", updated > 0 ? ApiCode.SUCCESS.getCode() : ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", updated > 0 ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 获取个体化建议下发列表
     */
    @GetMapping("/recommend/list")
    public Map<String, Object> recommendList(@RequestParam(required = false) String sourceType,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String riskLevel,
                                             @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            Long total = interventionRecommendMapper.countList(sourceType, status, riskLevel, keyword);
            List<Map<String, Object>> rows = interventionRecommendMapper.selectList(sourceType, status, riskLevel, keyword);

            // 兜底：部分历史记录未写入 risk_level，回填患者档案 riskLevel，保证前端能展示“危险程度/风险等级”
            if (rows != null && !rows.isEmpty()) {
                String rlFilter = riskLevel == null ? "" : riskLevel.trim().toUpperCase();
                Map<Long, String> patientRiskCache = new HashMap<>();
                Map<Long, String> alertSeverityCache = new HashMap<>();
                List<Map<String, Object>> patched = new ArrayList<>();
                for (Map<String, Object> r : rows) {
                    if (r == null) {
                        continue;
                    }
                    Object rlObj = r.get("riskLevel");
                    String rl = rlObj == null ? "" : String.valueOf(rlObj).trim();

                    // 统一：保证 riskLevel 键存在，避免前端渲染时出现空 badge
                    if (!r.containsKey("riskLevel")) {
                        r.put("riskLevel", rl);
                    }

                    if (rl.isEmpty()) {
                        Long pid = toLong(r.get("patientId"));
                        if (pid != null && pid > 0) {
                            String cached = patientRiskCache.get(pid);
                            if (cached == null) {
                                User patient = userMapper.findPatientById(pid);
                                cached = patient == null || patient.getRiskLevel() == null ? "" : String.valueOf(patient.getRiskLevel()).trim();
                                patientRiskCache.put(pid, cached);
                            }
                            if (cached != null && !cached.trim().isEmpty()) {
                                rl = cached.trim();
                                r.put("riskLevel", rl);
                            }
                        }
                    }

                    // 二次兜底：若来自 ALERT 且患者档案也无 riskLevel，则从预警 severity 回填
                    if (rl.isEmpty()) {
                        Object stObj = r.get("sourceType");
                        String st = stObj == null ? "" : String.valueOf(stObj).trim().toUpperCase();
                        if ("ALERT".equals(st)) {
                            Long sid = toLong(r.get("sourceId"));
                            if (sid != null && sid > 0) {
                                String cached = alertSeverityCache.get(sid);
                                if (cached == null) {
                                    try {
                                        cached = alertCenterMapper.findAlertSeverity(sid);
                                    } catch (Exception ignored) {
                                        cached = null;
                                    }
                                    cached = cached == null ? "" : String.valueOf(cached).trim();
                                    alertSeverityCache.put(sid, cached);
                                }
                                if (cached != null && !cached.trim().isEmpty()) {
                                    rl = cached.trim();
                                    r.put("riskLevel", rl);
                                }
                            }
                        }
                    }

                    // 如果调用方传了 riskLevel 过滤，但 SQL 因为空值未过滤掉，这里补一层过滤
                    if (!rlFilter.isEmpty()) {
                        String up = rl == null ? "" : rl.trim().toUpperCase();
                        if (!rlFilter.equals(up)) {
                            continue;
                        }
                    }
                    patched.add(r);
                }
                rows = patched;
                total = (long) patched.size();
            }

            if ((total == null || total == 0L) && (rows == null || rows.isEmpty())) {
                String st = sourceType == null ? "" : sourceType.trim().toUpperCase();
                String ss = status == null ? "" : status.trim().toUpperCase();
                if ((st.isEmpty() || "ALERT".equals(st)) && (ss.isEmpty() || "PENDING".equals(ss))) {
                    LocalDateTime startAt = LocalDateTime.now().minusDays(30);
                    LocalDateTime endAt = LocalDateTime.now().plusDays(1);
                    List<Map<String, Object>> alerts = alertCenterMapper.selectAlertCenterPage(null, keyword, null, null, null,
                            startAt, endAt, 0, 50);
                    List<Map<String, Object>> fallback = new ArrayList<>();

                    String rlFilter = riskLevel == null ? "" : riskLevel.trim().toUpperCase();
                    for (Map<String, Object> a : alerts == null ? Collections.<Map<String, Object>>emptyList() : alerts) {
                        if (a == null) {
                            continue;
                        }
                        Object patientIdObj = a.get("patientId");
                        Object alertIdObj = a.get("alertId");
                        Object srcObj = a.get("source");
                        Object riskObj = a.get("riskLevel");

                        Long patientId = patientIdObj == null ? null : toLong(patientIdObj);
                        Long sourceId = alertIdObj == null ? null : toLong(alertIdObj);
                        String source = srcObj == null ? "" : String.valueOf(srcObj).trim().toUpperCase();
                        String rl = riskObj == null ? "" : String.valueOf(riskObj).trim().toUpperCase();

                        if (!rlFilter.isEmpty() && !rlFilter.equals(rl)) {
                            continue;
                        }
                        if (patientId == null || sourceId == null) {
                            continue;
                        }

                        long syntheticId;
                        if ("DEVICE".equals(source)) {
                            syntheticId = -2000000000L - sourceId;
                        } else {
                            syntheticId = -1000000000L - sourceId;
                        }

                        Map<String, Object> x = new HashMap<>();
                        x.put("id", syntheticId);
                        x.put("sourceType", "ALERT");
                        x.put("sourceId", sourceId);
                        x.put("patientId", patientId);
                        x.put("patientName", a.get("patientName") == null ? "" : String.valueOf(a.get("patientName")));
                        x.put("riskLevel", rl);
                        x.put("triggerReason", a.get("summary") == null ? "" : String.valueOf(a.get("summary")));
                        Object at = a.get("alertTime");
                        x.put("triggerTime", at == null ? "" : String.valueOf(at).replace('T', ' '));
                        x.put("status", "PENDING");
                        x.put("sentTime", null);
                        x.put("bodyType", "");
                        x.put("pattern", "");
                        x.put("disease", "");
                        x.put("doctor", "");
                        x.put("recommendation", "");
                        fallback.add(x);
                    }

                    rows = fallback;
                    total = (long) fallback.size();
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("rows", rows == null ? Collections.emptyList() : rows);
            data.put("total", total == null ? 0L : total);

            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", "查询成功");
            result.put("data", data);
            return result;
        } catch (Exception ex) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + String.valueOf(ex.getMessage()));
            return result;
        }
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDateTime) {
            return (LocalDateTime) o;
        }
        String s = String.valueOf(o);
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.contains("T")) {
                s = s.replace('T', ' ');
            }
            if (s.length() == 16) {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            if (s.length() == 19) {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * AI 生成个性化建议
     */
    @PostMapping("/recommend/ai-suggest")
    public Map<String, Object> aiSuggest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (body == null) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "参数错误");
                return result;
            }

            String patientName = body.get("patientName") == null ? "患者" : String.valueOf(body.get("patientName"));
            String riskLevel = body.get("riskLevel") == null ? "" : String.valueOf(body.get("riskLevel"));
            String triggerReason = body.get("triggerReason") == null ? "" : String.valueOf(body.get("triggerReason"));
            String bodyType = body.get("bodyType") == null ? "" : String.valueOf(body.get("bodyType"));
            String pattern = body.get("pattern") == null ? "" : String.valueOf(body.get("pattern"));
            String disease = body.get("disease") == null ? "" : String.valueOf(body.get("disease"));

            String prompt = buildAiPrompt(patientName, riskLevel, triggerReason, bodyType, pattern, disease);
            String suggestionText;
            boolean fallback = false;

            String bodyApiKey = body.get("apiKey") == null ? null : String.valueOf(body.get("apiKey"));
            if (bodyApiKey == null || bodyApiKey.trim().isEmpty()) {
                bodyApiKey = body.get("apikey") == null ? null : String.valueOf(body.get("apikey"));
            }
            String headerApiKey = request == null ? null : request.getHeader("X-AI-API-KEY");
            String effectiveApiKey = (bodyApiKey != null && !bodyApiKey.trim().isEmpty())
                    ? bodyApiKey
                    : ((headerApiKey != null && !headerApiKey.trim().isEmpty()) ? headerApiKey : aiApiKey);

            if (effectiveApiKey == null || effectiveApiKey.trim().isEmpty()) {
                fallback = true;
                suggestionText = buildTemplateSuggestion(patientName, riskLevel, triggerReason, bodyType, pattern);
            } else {
                try {
                    suggestionText = callOpenAiChat(prompt, effectiveApiKey);
                    if (suggestionText == null || suggestionText.trim().isEmpty()) {
                        throw new IllegalStateException("AI 返回为空");
                    }
                } catch (Exception ex) {
                    fallback = true;
                    suggestionText = buildTemplateSuggestion(patientName, riskLevel, triggerReason, bodyType, pattern);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("text", suggestionText);
            data.put("recommendation", suggestionText);
            data.put("fallback", fallback);

            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", fallback ? "AI 未配置或调用失败，已生成模板建议" : "生成成功");
            result.put("data", data);
            return result;
        } catch (Exception ex) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "系统异常: " + String.valueOf(ex.getMessage()));
            return result;
        }
    }

    /**
     * 下发个性化建议
     */
    @PostMapping("/recommend/send")
    public Map<String, Object> sendRecommend(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Long id = toLong(body.get("id"));
        String recommendation = body.get("recommendation") == null ? null : String.valueOf(body.get("recommendation"));

        if (recommendation == null || recommendation.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "内容不能为空");
            return result;
        }

        InterventionRecommend rec = null;
        if (id != null && id > 0) {
            rec = interventionRecommendMapper.findById(id);
        }

        if (rec == null) {
            String sourceType = body.get("sourceType") == null ? null : String.valueOf(body.get("sourceType"));
            Long sourceId = toLong(body.get("sourceId"));
            Long patientId = toLong(body.get("patientId"));
            if (patientId == null || sourceType == null || sourceType.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "patientId / sourceType 不能为空");
                return result;
            }

            if (sourceId != null) {
                rec = interventionRecommendMapper.findBySource(sourceType.trim(), sourceId);
            }

            if (rec == null) {
                rec = new InterventionRecommend();
                rec.setSourceType(sourceType.trim().toUpperCase());
                rec.setSourceId(sourceId);
                rec.setPatientId(patientId);
                rec.setPatientName(body.get("patientName") == null ? null : String.valueOf(body.get("patientName")));
                rec.setRiskLevel(body.get("riskLevel") == null ? null : String.valueOf(body.get("riskLevel")));
                rec.setTriggerReason(body.get("triggerReason") == null ? null : String.valueOf(body.get("triggerReason")));
                rec.setTriggerTime(toLocalDateTime(body.get("triggerTime")));
                rec.setBodyType(body.get("bodyType") == null ? null : String.valueOf(body.get("bodyType")));
                rec.setPattern(body.get("pattern") == null ? null : String.valueOf(body.get("pattern")));
                rec.setDisease(body.get("disease") == null ? null : String.valueOf(body.get("disease")));
                rec.setDoctor(body.get("doctor") == null ? null : String.valueOf(body.get("doctor")));
                rec.setStatus("PENDING");
                rec.setRecommendation("");
                interventionRecommendMapper.insert(rec);
            }
            id = rec.getId();
        }

        int updated = interventionRecommendMapper.markSent(id, recommendation);
        if (updated <= 0) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "下发失败");
            return result;
        }

        rec = interventionRecommendMapper.findById(id);
        if (rec != null && rec.getPatientId() != null) {
            PatientMessage msg = new PatientMessage();
            msg.setPatientId(rec.getPatientId());
            msg.setTitle("个体化健康建议");
            msg.setContent(recommendation);
            msg.setMessageType("RECOMMENDATION");
            msg.setStatus("UNREAD");
            patientMessageMapper.insert(msg);

            try {
                DoctorAdvice advice = new DoctorAdvice();
                Long doctorId = AuthHeaderUtil.getUserId(request);
                if (doctorId != null) {
                    User u = userMapper.findById(doctorId);
                    if (u != null && u.getRole() != null && "DOCTOR".equalsIgnoreCase(u.getRole().trim())) {
                        advice.setDoctorId(u.getId());
                        advice.setDoctorName(u.getName() != null && !u.getName().trim().isEmpty() ? u.getName().trim() : u.getUsername());
                    }
                }
                if (advice.getDoctorName() == null || advice.getDoctorName().trim().isEmpty()) {
                    advice.setDoctorName(rec.getDoctor() == null ? "" : rec.getDoctor());
                }
                advice.setTitle("个体化健康建议");
                advice.setDescription(recommendation);
                String patientDisplay = rec.getPatientName() == null || rec.getPatientName().trim().isEmpty()
                        ? (rec.getPatientId() == null ? "" : String.valueOf(rec.getPatientId()))
                        : rec.getPatientName();
                advice.setPatientsJson("[\"" + patientDisplay.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]");
                doctorAdviceService.create(advice);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "下发成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/recommend/ensure")
    public Map<String, Object> ensureRecommend(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        String sourceType = body.get("sourceType") == null ? null : String.valueOf(body.get("sourceType"));
        Long sourceId = toLong(body.get("sourceId"));
        Long patientId = toLong(body.get("patientId"));
        if (patientId == null || sourceType == null || sourceType.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "patientId / sourceType 不能为空");
            return result;
        }

        InterventionRecommend record = null;
        if (sourceId != null) {
            record = interventionRecommendMapper.findBySource(sourceType.trim(), sourceId);
        }

        if (record == null) {
            record = new InterventionRecommend();
            record.setSourceType(sourceType.trim().toUpperCase());
            record.setSourceId(sourceId);
            record.setPatientId(patientId);
            record.setPatientName(body.get("patientName") == null ? null : String.valueOf(body.get("patientName")));
            record.setRiskLevel(body.get("riskLevel") == null ? null : String.valueOf(body.get("riskLevel")));
            record.setTriggerReason(body.get("triggerReason") == null ? null : String.valueOf(body.get("triggerReason")));
            record.setTriggerTime(toLocalDateTime(body.get("triggerTime")));
            record.setBodyType(body.get("bodyType") == null ? null : String.valueOf(body.get("bodyType")));
            record.setPattern(body.get("pattern") == null ? null : String.valueOf(body.get("pattern")));
            record.setDisease(body.get("disease") == null ? null : String.valueOf(body.get("disease")));
            record.setDoctor(body.get("doctor") == null ? null : String.valueOf(body.get("doctor")));
            record.setStatus("PENDING");
            record.setRecommendation("");
            interventionRecommendMapper.insert(record);
        } else {
            InterventionRecommend patch = new InterventionRecommend();
            patch.setId(record.getId());
            patch.setPatientId(patientId);
            patch.setPatientName(body.get("patientName") == null ? record.getPatientName() : String.valueOf(body.get("patientName")));
            patch.setRiskLevel(body.get("riskLevel") == null ? record.getRiskLevel() : String.valueOf(body.get("riskLevel")));
            patch.setTriggerReason(body.get("triggerReason") == null ? record.getTriggerReason() : String.valueOf(body.get("triggerReason")));
            patch.setTriggerTime(body.get("triggerTime") == null ? record.getTriggerTime() : toLocalDateTime(body.get("triggerTime")));
            patch.setBodyType(body.get("bodyType") == null ? record.getBodyType() : String.valueOf(body.get("bodyType")));
            patch.setPattern(body.get("pattern") == null ? record.getPattern() : String.valueOf(body.get("pattern")));
            patch.setDisease(body.get("disease") == null ? record.getDisease() : String.valueOf(body.get("disease")));
            patch.setDoctor(body.get("doctor") == null ? record.getDoctor() : String.valueOf(body.get("doctor")));
            interventionRecommendMapper.updateMeta(patch);
            record = interventionRecommendMapper.findById(record.getId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("sourceType", record.getSourceType());
        data.put("sourceId", record.getSourceId());
        data.put("patientId", record.getPatientId());
        data.put("patientName", record.getPatientName());
        data.put("riskLevel", record.getRiskLevel());
        data.put("triggerReason", record.getTriggerReason());
        data.put("triggerTime", record.getTriggerTime() == null ? null : record.getTriggerTime().toString().replace('T', ' '));
        data.put("status", record.getStatus());
        data.put("sentTime", record.getSentTime() == null ? null : record.getSentTime().toString().replace('T', ' '));
        data.put("bodyType", record.getBodyType());
        data.put("pattern", record.getPattern());
        data.put("disease", record.getDisease());
        data.put("doctor", record.getDoctor());
        data.put("recommendation", record.getRecommendation());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "OK");
        result.put("data", data);
        return result;
    }

    @PostMapping("/recommend/save")
    public Map<String, Object> saveRecommend(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Long id = toLong(body.get("id"));
        String recommendation = body.get("recommendation") == null ? "" : String.valueOf(body.get("recommendation"));

        InterventionRecommend rec = null;
        if (id != null && id > 0) {
            rec = interventionRecommendMapper.findById(id);
        }

        if (rec == null) {
            String sourceType = body.get("sourceType") == null ? null : String.valueOf(body.get("sourceType"));
            Long sourceId = toLong(body.get("sourceId"));
            Long patientId = toLong(body.get("patientId"));
            if (patientId == null || sourceType == null || sourceType.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.PARAM_ERROR.getCode());
                result.put("message", "patientId / sourceType 不能为空");
                return result;
            }

            if (sourceId != null) {
                rec = interventionRecommendMapper.findBySource(sourceType.trim(), sourceId);
            }

            if (rec == null) {
                rec = new InterventionRecommend();
                rec.setSourceType(sourceType.trim().toUpperCase());
                rec.setSourceId(sourceId);
                rec.setPatientId(patientId);
                rec.setPatientName(body.get("patientName") == null ? null : String.valueOf(body.get("patientName")));
                rec.setRiskLevel(body.get("riskLevel") == null ? null : String.valueOf(body.get("riskLevel")));
                rec.setTriggerReason(body.get("triggerReason") == null ? null : String.valueOf(body.get("triggerReason")));
                rec.setTriggerTime(toLocalDateTime(body.get("triggerTime")));
                rec.setBodyType(body.get("bodyType") == null ? null : String.valueOf(body.get("bodyType")));
                rec.setPattern(body.get("pattern") == null ? null : String.valueOf(body.get("pattern")));
                rec.setDisease(body.get("disease") == null ? null : String.valueOf(body.get("disease")));
                rec.setDoctor(body.get("doctor") == null ? null : String.valueOf(body.get("doctor")));
                rec.setStatus("PENDING");
                rec.setRecommendation("");
                interventionRecommendMapper.insert(rec);
            }
            id = rec.getId();
        }

        int updated = interventionRecommendMapper.updateDraft(id, recommendation);
        if (updated <= 0) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "保存失败");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "保存成功");
        result.put("data", data);
        return result;
    }

    /**
     * 生成随访任务清单
     */
    @PostMapping("/followup-tasks/generate")
    public Map<String, Object> generateFollowupTasks(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        Long userId = AuthHeaderUtil.getUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }

        User doctor = userMapper.findById(userId);
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        Long patientId = toLong(body.get("patientId"));
        String serviceType = body.get("serviceType") == null ? null : String.valueOf(body.get("serviceType"));
        String planDate = body.get("planDate") == null ? null : String.valueOf(body.get("planDate"));
        String priority = body.get("priority") == null ? null : String.valueOf(body.get("priority"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));

        if (patientId == null || serviceType == null || serviceType.trim().isEmpty() || planDate == null || planDate.trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "patientId / serviceType / planDate 不能为空");
            return result;
        }

        User patient = userMapper.findById(patientId);
        if (patient == null || patient.getRole() == null || !"PATIENT".equalsIgnoreCase(patient.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "patientId 不存在或不是患者");
            return result;
        }

        Long followUpUserId = userMapper.findFirstFollowUpUserId();
        if (followUpUserId == null) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "暂无随访人员，无法生成任务");
            return result;
        }

        FollowUpTask task = new FollowUpTask();
        task.setPatientId(patientId);
        task.setDoctorId(doctor.getId());
        task.setFollowUpUserId(followUpUserId);
        task.setTriggerType(serviceType);
        task.setDescription(remark);
        task.setStatus("ASSIGNED");
        task.setExt1(planDate);
        task.setExt2(priority);
        followUpTaskMapper.insert(task);

        LocalDateTime dueAt = LocalDateTime.now();
        try {
            LocalDate d = LocalDate.parse(planDate);
            dueAt = d.atStartOfDay();
        } catch (Exception ignored) {
        }

        FollowUpSchedule schedule = new FollowUpSchedule();
        schedule.setPatientId(patientId);
        schedule.setPlanId(null);
        schedule.setDueAt(dueAt);
        schedule.setStatus("DUE");
        schedule.setCompletedAt(null);
        schedule.setRelatedTaskId(task.getId());
        followUpScheduleMapper.insert(schedule);

        User followUpUser = userMapper.findById(followUpUserId);

        Map<String, Object> taskDto = new HashMap<>();
        taskDto.put("taskId", task.getId());
        taskDto.put("patientName", patient.getName());
        taskDto.put("address", patient.getAddress());
        taskDto.put("serviceType", serviceType);
        taskDto.put("planDate", planDate);
        taskDto.put("assignee", followUpUser == null ? "" : followUpUser.getName());
        taskDto.put("staffId", followUpUserId);
        taskDto.put("staffName", followUpUser == null ? "" : followUpUser.getName());
        taskDto.put("status", task.getStatus());
        taskDto.put("statusText", statusText(task.getStatus()));
        taskDto.put("remark", remark == null ? "" : remark);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("task", taskDto);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "生成成功");
        result.put("data", data);
        return result;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String statusText(String status) {
        if (status == null) {
            return "";
        }
        String s = status.trim().toUpperCase();
        if ("ASSIGNED".equals(s)) {
            return "待执行";
        }
        if ("IN_PROGRESS".equals(s)) {
            return "执行中";
        }
        if ("DONE".equals(s) || "COMPLETED".equals(s)) {
            return "已完成";
        }
        if ("CANCELLED".equals(s) || "CANCELED".equals(s)) {
            return "已取消";
        }
        return status;
    }
}
