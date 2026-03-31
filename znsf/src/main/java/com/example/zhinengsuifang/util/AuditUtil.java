package com.example.zhinengsuifang.util;

public class AuditUtil {

    private AuditUtil() {
    }

    public static String guessModule(String uri) {
        if (uri == null) {
            return "other";
        }
        String u = uri.trim();
        if (u.isEmpty()) {
            return "other";
        }
        int q = u.indexOf('?');
        if (q >= 0) {
            u = u.substring(0, q);
        }
        String[] parts = u.split("/");
        // expected: /api/<module>/...
        for (int i = 0; i < parts.length; i++) {
            if ("api".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                String m = parts[i + 1];
                if (m != null && !m.trim().isEmpty()) {
                    return m.trim();
                }
            }
        }
        return "other";
    }

    public static String safeTruncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        if (maxLen <= 0) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }

    public static String extractTarget(String uri, String query, String body) {
        String fromQuery = extractFirstId(query);
        if (fromQuery != null) {
            return fromQuery;
        }

        String fromBody = extractFirstId(body);
        if (fromBody != null) {
            return fromBody;
        }

        if (uri != null) {
            String u = uri;
            int q = u.indexOf('?');
            if (q >= 0) {
                u = u.substring(0, q);
            }
            String[] parts = u.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                String p = parts[i];
                if (p == null || p.trim().isEmpty()) {
                    continue;
                }
                String s = p.trim();
                boolean allDigits = true;
                for (int j = 0; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (c < '0' || c > '9') {
                        allDigits = false;
                        break;
                    }
                }
                if (allDigits) {
                    return "id=" + s;
                }
            }
        }
        return null;
    }

    public static String describeBusinessAction(String method, String uri) {
        String m = method == null ? "" : method.trim().toUpperCase();
        String u = uri == null ? "" : uri;
        int q = u.indexOf('?');
        if (q >= 0) {
            u = u.substring(0, q);
        }

        // 预警
        if (u.contains("/alert/createFollowUp")) {
            return "根据预警生成随访任务";
        }
        if (u.contains("/alert/mark")) {
            return "标记预警处理状态";
        }

        // 随访任务
        if (u.contains("/followUpTask/create")) {
            return "创建随访任务";
        }
        if (u.contains("/followUpTask/republish")) {
            return "重新发布随访任务";
        }
        if (u.contains("/followUpTask/cancel")) {
            return "取消随访任务";
        }
        if (u.contains("/api/followup/task/reschedule")) {
            return "随访任务改期";
        }

        // 患者端
        if (u.contains("/patient/basic-info") && "PUT".equals(m)) {
            return "更新患者基础信息";
        }

        // 干预/计划
        if (u.contains("/api/intervention/home-service/assign")) {
            return "指派随访人员";
        }
        if (u.contains("/api/intervention/visit-plan/update-status")) {
            return "更新复诊计划状态";
        }
        if (u.contains("/api/intervention/followup-tasks/generate")) {
            return "生成随访任务清单";
        }

        // 系统
        if (u.contains("/api/system")) {
            if (u.contains("/data-log")) {
                return "查看日志审计";
            }
            if (u.contains("/org-user")) {
                return "查看组织用户信息";
            }
        }

        // 兜底：按 HTTP 方法粗分
        if ("POST".equals(m)) {
            return "新增/提交";
        }
        if ("PUT".equals(m) || "PATCH".equals(m)) {
            return "更新/修改";
        }
        if ("DELETE".equals(m)) {
            return "删除";
        }
        if ("GET".equals(m)) {
            return "查询";
        }
        return "操作";
    }

    private static String extractFirstId(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }

        String[] keys = new String[]{"patientId", "taskId", "planId", "alertId", "id"};
        for (String key : keys) {
            String v = extractByKey(t, key);
            if (v != null) {
                return key + "=" + v;
            }
        }
        return null;
    }

    private static String extractByKey(String text, String key) {
        // query: key=123
        String q = key + "=";
        int idx = text.indexOf(q);
        if (idx >= 0) {
            int start = idx + q.length();
            int end = start;
            while (end < text.length()) {
                char c = text.charAt(end);
                if (c < '0' || c > '9') {
                    break;
                }
                end++;
            }
            if (end > start) {
                return text.substring(start, end);
            }
        }

        // json: "key":123 or "key":"123"
        String jsonKey = "\"" + key + "\"";
        idx = text.indexOf(jsonKey);
        if (idx >= 0) {
            int colon = text.indexOf(':', idx + jsonKey.length());
            if (colon > 0) {
                int p = colon + 1;
                while (p < text.length() && (text.charAt(p) == ' ' || text.charAt(p) == '\n' || text.charAt(p) == '\r' || text.charAt(p) == '\t')) {
                    p++;
                }
                if (p < text.length() && text.charAt(p) == '"') {
                    p++;
                    int end = p;
                    while (end < text.length()) {
                        char c = text.charAt(end);
                        if (c < '0' || c > '9') {
                            break;
                        }
                        end++;
                    }
                    if (end > p) {
                        return text.substring(p, end);
                    }
                } else {
                    int end = p;
                    while (end < text.length()) {
                        char c = text.charAt(end);
                        if (c < '0' || c > '9') {
                            break;
                        }
                        end++;
                    }
                    if (end > p) {
                        return text.substring(p, end);
                    }
                }
            }
        }
        return null;
    }
}
