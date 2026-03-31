package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.DoctorAdvice;
import com.example.zhinengsuifang.entity.OperationAuditLog;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.entity.InterventionRecommend;
import com.example.zhinengsuifang.mapper.InterventionRecommendMapper;
import com.example.zhinengsuifang.service.DoctorAdviceService;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.OperationAuditLogService;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@Tag(name = "系统", description = "系统与基础配置相关接口")
public class ApiSystemController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private OperationAuditLogService operationAuditLogService;

    @Resource
    private DoctorAdviceService doctorAdviceService;

    @Resource
    private InterventionRecommendMapper interventionRecommendMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/rules")
    public Map<String, Object> rules() {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> seasonRules = new ArrayList<>();
        seasonRules.add(rule("寒冷季节", "显著增加心血管事件风险"));
        seasonRules.add(rule("梅雨", "可能加重呼吸道症状"));
        seasonRules.add(rule("高温", "可能增加脱水与心率波动风险"));

        Map<String, Object> data = new HashMap<>();
        data.put("seasonRules", seasonRules);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/doctor-advice/detail")
    public Map<String, Object> doctorAdviceDetail(@RequestParam(required = false) Long id,
                                                  HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }
        if (id == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "缺少参数: id");
            return result;
        }

        Map<String, Object> data = null;

        InterventionRecommend rec = interventionRecommendMapper.findById(id);
        if (rec != null) {
            data = new HashMap<>();
            data.put("id", rec.getId());
            LocalDateTime at = rec.getSentTime() != null ? rec.getSentTime() : rec.getCreatedAt();
            data.put("adviceDate", at == null ? null : at.toString().replace('T', ' '));
            data.put("doctorName", rec.getDoctor() == null ? "" : rec.getDoctor());
            data.put("title", "个体化健康建议");
            data.put("description", rec.getRecommendation() == null ? "" : rec.getRecommendation());
            List<String> patients = new ArrayList<>();
            if (rec.getPatientName() != null && !rec.getPatientName().trim().isEmpty()) {
                patients.add(rec.getPatientName().trim());
            } else if (rec.getPatientId() != null) {
                patients.add(String.valueOf(rec.getPatientId()));
            }
            data.put("patients", patients);
        }

        if (data == null) {
            DoctorAdvice a = doctorAdviceService.getById(id);
            if (a == null) {
                result.put("success", false);
                result.put("code", ApiCode.NOT_FOUND.getCode());
                result.put("message", "记录不存在");
                return result;
            }
            data = new HashMap<>();
            data.put("id", a.getId());
            data.put("adviceDate", a.getAdviceDate() == null ? null : a.getAdviceDate().toString().replace('T', ' '));
            data.put("doctorId", a.getDoctorId());
            data.put("doctorName", a.getDoctorName());
            data.put("title", a.getTitle());
            data.put("description", a.getDescription());
            data.put("patients", parsePatients(a.getPatientsJson()));
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private List<String> parsePatients(String patientsJson) {
        if (patientsJson == null || patientsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String raw = patientsJson.trim();
        try {
            // 兼容 JSON 数组格式：["张三","李四"]
            if (raw.startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<?> arr = om.readValue(raw, java.util.List.class);
                if (arr == null) {
                    return Collections.emptyList();
                }
                List<String> out = new ArrayList<>();
                for (Object o : arr) {
                    if (o == null) continue;
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) out.add(s);
                }
                return out;
            }
        } catch (Exception ignored) {
        }

        // 兜底：逗号分隔
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    @GetMapping("/doctor-advice")
    public Map<String, Object> doctorAdvice(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            HttpServletRequest request) {
        int p = page == null ? 1 : Math.max(1, page);
        int ps = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 200));
        int offset = (p - 1) * ps;

        // 可选：仅登录用户可查看（和 data-log 一致）
        User operator = requireOperator(request);
        if (operator == null) {
            Map<String, Object> fail = new HashMap<>();
            fail.put("success", false);
            fail.put("code", ApiCode.UNAUTHORIZED.getCode());
            fail.put("message", "未登录或登录已过期");
            return fail;
        }

        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                startAt = LocalDateTime.parse(startDate.trim());
            }
        } catch (Exception ignored) {
        }
        try {
            if (endDate != null && !endDate.trim().isEmpty()) {
                endAt = LocalDateTime.parse(endDate.trim());
            }
        } catch (Exception ignored) {
        }

        Long total = interventionRecommendMapper.countSent(keyword, startAt, endAt);
        List<Map<String, Object>> rows = interventionRecommendMapper.selectSentPage(keyword, startAt, endAt, offset, ps);

        List<Map<String, Object>> resultRows = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> x : rows) {
                if (x == null) {
                    continue;
                }
                Map<String, Object> r = new HashMap<>();
                r.put("id", x.get("id"));
                r.put("adviceDate", x.get("adviceDate"));
                r.put("doctorName", x.get("doctor") == null ? "" : String.valueOf(x.get("doctor")));
                r.put("title", "个体化健康建议");
                r.put("description", x.get("recommendation") == null ? "" : String.valueOf(x.get("recommendation")));
                List<String> patients = new ArrayList<>();
                Object pn = x.get("patientName");
                Object pid = x.get("patientId");
                if (pn != null && !String.valueOf(pn).trim().isEmpty()) {
                    patients.add(String.valueOf(pn).trim());
                } else if (pid != null && !String.valueOf(pid).trim().isEmpty()) {
                    patients.add(String.valueOf(pid).trim());
                }
                r.put("patients", patients);
                resultRows.add(r);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total == null ? 0L : total);
        data.put("rows", resultRows);
        data.put("page", p);
        data.put("pageSize", ps);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/analysis-summary")
    public Map<String, Object> analysisSummary() {
        Map<String, Object> data = new HashMap<>();
        data.put("followupSummary", "随访完成率总体稳定，建议关注高危患者随访及时性。");
        data.put("riskStructureSummary", "当前高危患者占比存在波动，建议加强风险分层随访策略。");
        data.put("diseaseDistributionSummary", "常见慢病以高血压、糖尿病为主。");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/change-logs")
    public Map<String, Object> changeLogs(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer pageSize,
                                          @RequestParam(required = false) String module,
                                          @RequestParam(required = false) Long operatorId,
                                          @RequestParam(required = false) LocalDateTime startTime,
                                          @RequestParam(required = false) LocalDateTime endTime) {
        int p = page == null ? 1 : Math.max(1, page);
        int ps = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 200));

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(log(1L, module == null ? "followup" : module, "update", 1001L, "系统示例日志", LocalDateTime.now()));

        Map<String, Object> data = new HashMap<>();
        data.put("total", rows.size());
        data.put("rows", rows.subList(0, Math.min(ps, rows.size())));
        data.put("page", p);
        data.put("pageSize", ps);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> rule(String factorName, String weightDesc) {
        Map<String, Object> r = new HashMap<>();
        r.put("factorName", factorName);
        r.put("weightDesc", weightDesc);
        return r;
    }

    private Map<String, Object> advice(Long id, String title, String summary, String disease, String tag) {
        Map<String, Object> a = new HashMap<>();
        a.put("id", id);
        a.put("title", title);
        a.put("summary", summary);
        a.put("disease", disease == null ? "" : disease);
        List<String> tags = new ArrayList<>();
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag);
        }

        a.put("tags", tags);
        a.put("createdAt", LocalDateTime.now());
        return a;
    }

    @RequestMapping(value = "/org-user", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> orgUser(HttpServletRequest request,
                                       @RequestParam(required = false) String doctorUsername,
                                       @RequestParam(required = false) String doctorPassword,
                                       @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        // 1) Header 鉴权优先
        User operator = requireOperator(request);

        // 2) Header 鉴权失败则回退到 doctorUsername/doctorPassword（兼容旧前端）
        if (operator == null) {
            String fallbackUsername = firstNonBlank(
                    doctorUsername,
                    request == null ? null : request.getParameter("doctorUsername"),
                    request == null ? null : request.getParameter("username"),
                    body == null ? null : (body.get("doctorUsername") == null ? null : String.valueOf(body.get("doctorUsername"))),
                    body == null ? null : (body.get("username") == null ? null : String.valueOf(body.get("username")))
            );
            String fallbackPassword = firstNonBlank(
                    doctorPassword,
                    request == null ? null : request.getParameter("doctorPassword"),
                    request == null ? null : request.getParameter("password"),
                    body == null ? null : (body.get("doctorPassword") == null ? null : String.valueOf(body.get("doctorPassword"))),
                    body == null ? null : (body.get("password") == null ? null : String.valueOf(body.get("password")))
            );
            if (fallbackUsername == null || fallbackUsername.trim().isEmpty() || fallbackPassword == null || fallbackPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "未登录或登录已过期");
                return result;
            }

            User doctor = userMapper.findByUsername(fallbackUsername.trim());
            if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
                result.put("success", false);
                result.put("code", ApiCode.FORBIDDEN.getCode());
                result.put("message", "無权限");
                return result;
            }

            boolean match = passwordEncoder.matches(fallbackPassword, doctor.getPassword());
            if (!match) {
                result.put("success", false);
                result.put("code", ApiCode.UNAUTHORIZED.getCode());
                result.put("message", "医生账号或密码错誤");
                return result;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deptList", new ArrayList<>());

        List<Map<String, Object>> rawDoctors = userMapper.selectDoctorsForOrgUser();
        List<Map<String, Object>> userList = new ArrayList<>();
        if (rawDoctors != null) {
            for (Map<String, Object> r : rawDoctors) {
                if (r == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>(r);
                Object id = item.get("id");
                Object name = item.get("name");
                Object username = item.get("username");
                String display = firstNonBlank(
                        name == null ? null : String.valueOf(name),
                        username == null ? null : String.valueOf(username)
                );
                item.put("value", id);
                item.put("label", display == null ? "" : display);
                item.put("text", display == null ? "" : display);
                userList.add(item);
            }
        }
        data.put("userList", userList);
        data.put("doctorList", userList);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/data-log")
    public Map<String, Object> dataLog(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        User operator = requireOperator(request);
        if (operator == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "未登录或登录已过期");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("dictList", new ArrayList<>());
        data.put("logList", new ArrayList<>());

        // 医生建议留存（最近 200 条）
        List<DoctorAdvice> latestAdvice = doctorAdviceService.latest(200);
        List<Map<String, Object>> doctorRecommendations = new ArrayList<>();
        if (latestAdvice != null) {
            for (DoctorAdvice a : latestAdvice) {
                if (a == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("date", a.getAdviceDate() == null ? "" : a.getAdviceDate().toString().replace('T', ' '));
                item.put("doctor", a.getDoctorName() == null ? "" : a.getDoctorName());
                item.put("title", a.getTitle() == null ? "" : a.getTitle());
                item.put("description", a.getDescription() == null ? "" : a.getDescription());
                item.put("patients", new ArrayList<>());
                doctorRecommendations.add(item);
            }
        }
        data.put("doctorRecommendations", doctorRecommendations);

        List<OperationAuditLog> latest = operationAuditLogService.latest(200);
        List<Map<String, Object>> modificationLogs = new ArrayList<>();
        if (latest != null) {
            long idSeq = 1;
            for (OperationAuditLog l : latest) {
                if (l == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("id", l.getId() != null ? l.getId() : (idSeq++));
                String type = l.getModule() == null ? "other" : l.getModule();
                item.put("type", type);
                item.put("operator", l.getOperatorName() == null ? "" : l.getOperatorName());
                item.put("time", l.getCreatedAt() == null ? "" : l.getCreatedAt().toString().replace('T', ' '));
                String action = l.getAction();
                if (action == null || action.trim().isEmpty()) {
                    action = (l.getRequestMethod() == null ? "" : l.getRequestMethod().toUpperCase()) + " " + (l.getRequestUri() == null ? "" : l.getRequestUri());
                }
                item.put("action", action == null ? "" : action.trim());
                item.put("target", l.getTarget() == null ? "" : l.getTarget());
                item.put("description", l.getSuccess() != null && l.getSuccess() == 1 ? "" : (l.getErrorMessage() == null ? "失败" : l.getErrorMessage()));
                modificationLogs.add(item);
            }
        }
        data.put("modificationLogs", modificationLogs);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    private User requireOperator(HttpServletRequest request) {
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v == null) {
                continue;
            }
            String s = v.trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private Map<String, Object> log(Long id, String module, String action, Long targetId, String summary, LocalDateTime createdAt) {
        Map<String, Object> l = new HashMap<>();
        l.put("id", id);
        l.put("module", module);
        l.put("action", action);
        l.put("targetId", targetId);
        l.put("operatorName", "system");
        l.put("summary", summary);
        l.put("createdAt", createdAt);
        return l;
    }
}
