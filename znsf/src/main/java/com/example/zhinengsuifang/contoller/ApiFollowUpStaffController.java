package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.CreateUserByDoctorRequest;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.AuthService;
import com.example.zhinengsuifang.util.ValidationUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/followup")
@Tag(name = "随访人员", description = "随访人员管理相关接口")
public class ApiFollowUpStaffController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @Resource
    private AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping({"/staff", "/staff/list"})
    public Map<String, Object> staffList(jakarta.servlet.http.HttpServletRequest request,
                                         @RequestParam(required = false) String doctorUsername,
                                         @RequestParam(required = false) String doctorPassword,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer pageNo,
                                         @RequestParam(required = false) Integer pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String staffName,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) String statusStr) {
        Map<String, Object> result = new HashMap<>();

        // 优先使用 Header 认证
        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        User currentUser = userId != null ? userMapper.findById(userId) : null;

        // 如果 Header 认证失败，回退到参数认证
        if (currentUser == null || !"DOCTOR".equalsIgnoreCase(currentUser.getRole())) {
            if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "医生账号或密码不能为空");
                return result;
            }

            User doctor = userMapper.findByUsername(doctorUsername.trim());
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

        // 兼容前端不同参数名
        int p = page != null ? page : (pageNo != null ? pageNo : 1);
        p = Math.max(1, p);
        // 前端“随访人员管理”页面需要一次性拉取全部人员做本地筛选，默认放大 pageSize。
        // 同时设置一个合理上限，避免误传极大值导致接口压力过大。
        int ps = pageSize == null ? 2000 : Math.max(1, Math.min(pageSize, 2000));
        int offset = (p - 1) * ps;

        String kw = keyword != null ? keyword : staffName;
        Integer st = status;
        if (st == null && statusStr != null && !statusStr.isEmpty()) {
            try {
                st = Integer.parseInt(statusStr);
            } catch (NumberFormatException ignored) {}
        }

        Long total = userMapper.countFollowUpStaff(kw, st);
        List<Map<String, Object>> rows = userMapper.selectFollowUpStaffPage(kw, st, offset, ps);

        Long totalStaff = userMapper.countFollowUpStaff(kw, st);
        Long activeStaff = userMapper.countFollowUpStaff(kw, 1);
        Long activeThisMonth = userMapper.countActiveStaffThisMonth(kw, 1);
        Long totalFollowups = userMapper.countFollowupRecordsForStaff(kw, st);
        Long monthFollowups = userMapper.countFollowupRecordsForStaffThisMonth(kw, st);
        Long totalPatients = userMapper.countDistinctAssignedPatientsForStaff(kw, st);

        long ts = totalStaff == null ? 0L : totalStaff;
        long tf = totalFollowups == null ? 0L : totalFollowups;
        long tp = totalPatients == null ? 0L : totalPatients;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStaff", ts);
        stats.put("activeStaff", activeStaff == null ? 0L : activeStaff);
        stats.put("activeThisMonth", activeThisMonth == null ? 0L : activeThisMonth);
        stats.put("totalFollowups", tf);
        stats.put("monthFollowups", monthFollowups == null ? 0L : monthFollowups);
        stats.put("avgFollowups", ts <= 0 ? 0 : Math.round((double) tf / (double) ts));
        stats.put("totalPatients", tp);
        stats.put("avgPatients", ts <= 0 ? 0 : Math.round((double) tp / (double) ts));

        Map<String, Object> data = new HashMap<>();
        long totalVal = total == null ? 0L : total;
        long totalPages = ps <= 0 ? 1 : (long) Math.ceil(totalVal * 1.0 / ps);
        data.put("pageNo", p);
        data.put("pageSize", ps);
        data.put("totalPages", totalPages);
        data.put("total", totalVal);
        data.put("rows", rows == null ? Collections.emptyList() : rows);
        data.put("stats", stats);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @GetMapping("/staff/detail")
    public Map<String, Object> staffDetail(jakarta.servlet.http.HttpServletRequest request,
                                           @RequestParam(required = false) String doctorUsername,
                                           @RequestParam(required = false) String doctorPassword,
                                           @RequestParam(required = false) Long id) {
        Map<String, Object> result = new HashMap<>();

        if (id == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "id不能为空");
            return result;
        }

        // 优先使用 Header 认证
        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        User currentUser = userId != null ? userMapper.findById(userId) : null;

        // 如果 Header 认证失败，回退到参数认证
        if (currentUser == null || !"DOCTOR".equalsIgnoreCase(currentUser.getRole())) {
            if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "医生账号或密码不能为空");
                return result;
            }

            User doctor = userMapper.findByUsername(doctorUsername.trim());
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

        User staff = userMapper.findById(id);
        if (staff == null || staff.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(staff.getRole())) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "随访人员不存在");
            return result;
        }

        Long patientCount = userMapper.countAssignedPatientCountForStaff(id);
        Long totalFollowupCount = userMapper.countFollowupCountForStaff(id);
        Long monthFollowupCount = userMapper.countMonthFollowupCountForStaff(id);
        Long completedCount = userMapper.countCompletedFollowupCountForStaff(id);

        List<Map<String, Object>> patients = userMapper.selectAssignedPatientsForStaff(id, 200);
        List<Map<String, Object>> recentFollowupsRaw = userMapper.selectRecentFollowupsForStaff(id, 10);
        List<Map<String, Object>> recentFollowups = new ArrayList<>();
        if (recentFollowupsRaw != null) {
            for (Map<String, Object> row : recentFollowupsRaw) {
                if (row == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>(row);
                String resultStatusCode = firstNonBlank(row.get("resultStatus"));
                item.put("resultStatusCode", resultStatusCode);
                item.put("resultStatus", mapFollowupResultText(resultStatusCode));
                recentFollowups.add(item);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", staff.getId());
        data.put("name", staff.getName());
        data.put("jobNo", staff.getUsername());
        data.put("mobile", staff.getPhone());
        data.put("status", staff.getStatus());
        data.put("orgName", "");
        data.put("deptName", staff.getAddress() == null ? "" : staff.getAddress());
        data.put("department", staff.getAddress() == null ? "" : staff.getAddress());
        data.put("position", "随访人员");
        data.put("postName", "随访人员");
        data.put("role", staff.getRole());
        data.put("patientCount", patientCount == null ? 0L : patientCount);
        data.put("totalFollowupCount", totalFollowupCount == null ? 0L : totalFollowupCount);
        data.put("monthFollowupCount", monthFollowupCount == null ? 0L : monthFollowupCount);
        data.put("completedCount", completedCount == null ? 0L : completedCount);
        data.put("patients", patients == null ? Collections.emptyList() : patients);
        data.put("recentFollowups", recentFollowups == null ? Collections.emptyList() : recentFollowups);

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/staff")
    public Map<String, Object> createStaff(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        // 优先使用 Header 认证
        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        User currentUser = userId != null ? userMapper.findById(userId) : null;

        // 如果 Header 认证成功且是医生角色，直接使用
        if (currentUser != null && "DOCTOR".equalsIgnoreCase(currentUser.getRole())) {
            // 直接创建用户，不需要再验证密码
            String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
            String phone = body.get("phone") == null ? null : String.valueOf(body.get("phone"));

            if (name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "姓名不能为空");
                return result;
            }
            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "手机号不能为空");
                return result;
            }

            String normalizedPhone = ValidationUtil.normalizeCnMobile(phone);
            if (!ValidationUtil.isValidCnMobile(normalizedPhone)) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "手机号格式不合法");
                return result;
            }

            // 检查手机号是否已存在
            User phoneExist = userMapper.findByPhone(normalizedPhone);
            if (phoneExist != null) {
                result.put("success", false);
                result.put("code", ApiCode.CONFLICT.getCode());
                result.put("message", "手机号已存在");
                return result;
            }

            // 创建新用户
            User newUser = new User();
            newUser.setUsername(null);
            newUser.setName(name.trim());
            newUser.setPhone(normalizedPhone);
            newUser.setAddress(body.get("address") == null ? null : String.valueOf(body.get("address")));
            newUser.setSex(body.get("sex") == null ? null : String.valueOf(body.get("sex")));
            Object ageObj = body.get("age");
            if (ageObj instanceof Number) {
                newUser.setAge(((Number) ageObj).intValue());
            }
            newUser.setRole("FOLLOW_UP");
            newUser.setStatus(1);
            newUser.setPassword(passwordEncoder.encode("123456"));

            userMapper.insert(newUser);

            Long newUserId = newUser.getId();
            if (newUserId == null) {
                User created = userMapper.findByPhone(newUser.getPhone());
                if (created != null) {
                    newUserId = created.getId();
                }
            }

            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", "创建成功");
            result.put("userId", newUserId);
            return result;
        }

        // 回退到原有的 body 传参方式
        CreateUserByDoctorRequest req = new CreateUserByDoctorRequest();
        req.setDoctorUsername(body.get("doctorUsername") == null ? null : String.valueOf(body.get("doctorUsername")));
        req.setDoctorPassword(body.get("doctorPassword") == null ? null : String.valueOf(body.get("doctorPassword")));
        req.setUsername(body.get("username") == null ? null : String.valueOf(body.get("username")));
        req.setName(body.get("name") == null ? null : String.valueOf(body.get("name")));
        req.setPhone(body.get("phone") == null ? null : String.valueOf(body.get("phone")));
        req.setAddress(body.get("address") == null ? null : String.valueOf(body.get("address")));
        req.setSex(body.get("sex") == null ? null : String.valueOf(body.get("sex")));
        Object ageObj = body.get("age");
        if (ageObj instanceof Number) {
            req.setAge(((Number) ageObj).intValue());
        }
        req.setRole("FOLLOW_UP");
        req.setRiskLevel(null);

        return authService.createUserByDoctor(req);
    }

    @PostMapping("/staff/status")
    public Map<String, Object> updateStaffStatus(jakarta.servlet.http.HttpServletRequest request,
                                                 @RequestParam(required = false) String doctorUsername,
                                                 @RequestParam(required = false) String doctorPassword,
                                                 @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        Object idObj = body.get("id");
        if (idObj == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "id不能为空");
            return result;
        }

        Long id;
        try {
            id = Long.parseLong(String.valueOf(idObj));
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "id不合法");
            return result;
        }

        Integer status = 0;
        Object statusObj = body.get("status");
        if (statusObj != null) {
            try {
                status = Integer.parseInt(String.valueOf(statusObj));
            } catch (NumberFormatException ignored) {
            }
        }

        if (status == null || (status != 0 && status != 1)) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "status不合法，仅支持0/1");
            return result;
        }

        boolean force = false;
        Object forceObj = body.get("force");
        if (forceObj != null) {
            String fv = String.valueOf(forceObj).trim();
            force = "1".equals(fv) || "true".equalsIgnoreCase(fv) || "yes".equalsIgnoreCase(fv);
        }

        // 优先使用 Header 认证
        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        User currentUser = userId != null ? userMapper.findById(userId) : null;

        // 如果 Header 认证失败，回退到参数认证
        if (currentUser == null || !"DOCTOR".equalsIgnoreCase(currentUser.getRole())) {
            if (doctorUsername == null || doctorUsername.trim().isEmpty() || doctorPassword == null || doctorPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "医生账号或密码不能为空");
                return result;
            }

            User doctor = userMapper.findByUsername(doctorUsername.trim());
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

        User staff = userMapper.findById(id);
        if (staff == null || staff.getRole() == null || !"FOLLOW_UP".equalsIgnoreCase(staff.getRole())) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "随访人员不存在");
            return result;
        }

        int updated = userMapper.updateStatusById(id, status);
        if (updated <= 0) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "更新失败");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "更新成功");
        return result;
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

}
