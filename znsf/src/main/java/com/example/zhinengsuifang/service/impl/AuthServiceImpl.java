package com.example.zhinengsuifang.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.zhinengsuifang.dto.ChangePasswordRequest;
import com.example.zhinengsuifang.dto.CreateUserByDoctorRequest;
import com.example.zhinengsuifang.dto.LoginByIdRequest;
import com.example.zhinengsuifang.dto.LoginByPhoneRequest;
import com.example.zhinengsuifang.dto.RegisterRequest;
import com.example.zhinengsuifang.entity.PatientBasicInfo;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.PatientBasicInfoMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.AuthService;
import com.example.zhinengsuifang.util.ValidationUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证与用户创建相关的业务实现。
 * <p>
 * 包含：
 * 1) 医生注册（仅允许 DOCTOR）
 * 2) 医生创建患者/随访员账号
 * 3) 账号密码登录校验
 * </p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PatientBasicInfoMapper patientBasicInfoMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    /**
     * 用户注册。
     * <p>
     * 约束：
     * 1) username/password 必填
     * 2) confirmPassword 必须与 password 一致
     * 3) phone 可选
     * </p>
     *
     * @param request 注册用户信息
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> register(RegisterRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "姓名不能为空");
            return result;
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "密码不能为空");
            return result;
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "请再次输入密码");
            return result;
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "两次输入的密码不一致");
            return result;
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "手机号不能为空");
            return result;
        }

        String phone = ValidationUtil.normalizeCnMobile(request.getPhone());
        if (!ValidationUtil.isValidCnMobile(phone)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "手机号格式不合法");
            return result;
        }
        User phoneExist = userMapper.findByPhone(phone);
        if (phoneExist != null) {
            result.put("success", false);
            result.put("code", ApiCode.CONFLICT.getCode());
            result.put("message", "手机号已存在");
            return result;
        }

        User user = new User();
        user.setUsername(null);
        user.setName(request.getName().trim());
        user.setPhone(phone);
        user.setRole("DOCTOR");
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userMapper.insert(user);

        Long userId = user.getId();
        if (userId == null) {
            User created = userMapper.findByPhone(user.getPhone());
            if (created != null) {
                userId = created.getId();
            }
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "注册成功");
        result.put("userId", userId);
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return result;
    }

    @Override
    /**
     * 医生创建新用户（患者/随访员）。
     * <p>
     * 流程：
     * 1) 校验医生身份（role=DOCTOR 并且密码匹配）
     * 2) 校验新用户必填字段与角色合法性
     * 3) 如为患者，额外校验 riskLevel
     * 4) 写库后返返新用户 userId
     * </p>
     *
     * @param request 创建用户请求
     * @return 统一返返结构：success/message/userId
     */
    @Transactional
    public Map<String, Object> createUserByDoctor(CreateUserByDoctorRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        if ((request.getDoctorId() == null && (request.getDoctorUsername() == null || request.getDoctorUsername().trim().isEmpty()))
                || request.getDoctorPassword() == null || request.getDoctorPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "医生账号或密码不能为空");
            return result;
        }

        // 先查找医生账号，仅允许 DOCTOR 执行创建操作（优先 doctorId，其次 doctorUsername）
        User doctor = request.getDoctorId() != null
                ? userMapper.findById(request.getDoctorId())
                : userMapper.findByUsername(request.getDoctorUsername().trim());
        if (doctor == null || doctor.getRole() == null || !"DOCTOR".equalsIgnoreCase(doctor.getRole().trim())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        // 密码使用 BCrypt 校验（明文 request 密码 vs DB 密文）
        boolean doctorMatch = passwordEncoder.matches(request.getDoctorPassword(), doctor.getPassword());
        if (!doctorMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "医生账号或密码错誤");
            return result;
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "姓名不能为空");
            return result;
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "手机号不能为空");
            return result;
        }

        String newUserPhone = ValidationUtil.normalizeCnMobile(request.getPhone());
        if (!ValidationUtil.isValidCnMobile(newUserPhone)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "手机号格式不合法");
            return result;
        }
        User phoneExist = userMapper.findByPhone(newUserPhone);
        if (phoneExist != null) {
            result.put("success", false);
            result.put("code", ApiCode.CONFLICT.getCode());
            result.put("message", "手机号已存在");
            return result;
        }

        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "角色不能为空");
            return result;
        }

        // 统一角色格式，避免大小写不一致
        String role = request.getRole().trim().toUpperCase();
        if (!"PATIENT".equals(role) && !"FOLLOW_UP".equals(role)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "仅允许创建患者或随访员");
            return result;
        }

        String normalizedIdCard = null;
        if ("PATIENT".equals(role)) {
            if (request.getIdCard() == null || request.getIdCard().trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "身份证号不能为空");
                return result;
            }
            normalizedIdCard = ValidationUtil.normalizeIdCard(request.getIdCard());
            if (!ValidationUtil.isValidChineseIdCard18(normalizedIdCard)) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "身份证号格式不合法");
                return result;
            }
        }

        User newUser = new User();
        newUser.setUsername(null);
        newUser.setName(request.getName());
        newUser.setAddress(request.getAddress());
        newUser.setAge(request.getAge());
        newUser.setSex(request.getSex());
        newUser.setPhone(newUserPhone);
        // 患者需要补充风险等级；随访员不需要
        if ("PATIENT".equals(role)) {
            String riskLevel = request.getRiskLevel();
            if (riskLevel == null || riskLevel.trim().isEmpty()) {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "风险等级不能为空");
                return result;
            }

            String rl = riskLevel.trim();
            String normalized;
            String upper = rl.toUpperCase();
            if ("LOW".equals(upper) || "MID".equals(upper) || "HIGH".equals(upper)) {
                normalized = upper;
            } else if ("MEDIUM".equals(upper)) {
                normalized = "MID";
            } else if ("低".equals(rl)) {
                normalized = "LOW";
            } else if ("中".equals(rl)) {
                normalized = "MID";
            } else if ("高".equals(rl)) {
                normalized = "HIGH";
            } else {
                result.put("success", false);
                result.put("code", ApiCode.VALIDATION_ERROR.getCode());
                result.put("message", "风险等级只能为：LOW/MID/HIGH");
                return result;
            }

            newUser.setRiskLevel(normalized);
        }
        newUser.setRole(role);
        newUser.setStatus(1);
        // 新建用户默认密码（已加密存储）
        newUser.setPassword(passwordEncoder.encode("123456"));

        userMapper.insert(newUser);

        // 某些 Mapper/DB 配置下 insert 后不会自动返填 id，因此做一次兜底查询
        Long newUserId = newUser.getId();
        if (newUserId == null) {
            User created = userMapper.findByPhone(newUser.getPhone());
            if (created != null) {
                newUserId = created.getId();
            }
        }

        if (newUserId == null) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "创建失败");
            return result;
        }

        if ("PATIENT".equals(role)) {
            PatientBasicInfo basicInfo = new PatientBasicInfo();
            basicInfo.setPatientId(newUserId);
            basicInfo.setIdCard(normalizedIdCard);
            basicInfo.setExt1(String.valueOf(doctor.getId()));
            String doctorName = doctor.getName();
            if (doctorName == null || doctorName.trim().isEmpty()) {
                doctorName = doctor.getUsername();
            }
            basicInfo.setExt2(doctorName == null ? null : doctorName.trim());

            String disease = request.getDisease();
            if (disease != null) {
                disease = disease.trim();
                if (disease.isEmpty()) {
                    disease = null;
                }
            }
            String syndrome = request.getSyndrome();
            if (syndrome != null) {
                syndrome = syndrome.trim();
                if (syndrome.isEmpty()) {
                    syndrome = null;
                }
            }
            basicInfo.setExt3(disease);
            basicInfo.setExt4(syndrome);

             String constitution = request.getConstitution();
             if (constitution != null) {
                 constitution = constitution.trim();
                 if (constitution.isEmpty()) {
                     constitution = null;
                 }
             }
             String familyHistory = request.getFamilyHistory();
             if (familyHistory != null) {
                 familyHistory = familyHistory.trim();
                 if (familyHistory.isEmpty()) {
                     familyHistory = null;
                 }
             }
             if (constitution != null || familyHistory != null) {
                 try {
                     Map<String, Object> ext = new HashMap<>();
                     ext.put("constitution", constitution);
                     ext.put("familyHistory", familyHistory);
                     basicInfo.setExt5(OBJECT_MAPPER.writeValueAsString(ext));
                 } catch (Exception ignored) {
                     basicInfo.setExt5(null);
                 }
             }

            patientBasicInfoMapper.insert(basicInfo);
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "创建成功");
        result.put("userId", newUserId);
        result.put("username", newUser.getUsername());
        return result;
    }

    @Override
    /**
     * 登录校验。
     *
     * @param username 用户名
     * @param rawPassword 明文密码
     * @return 统一返返结构：success/message/userId/role/name
     */
    public Map<String, Object> login(String username, String rawPassword) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "账号或密码错誤");
            return result;
        }

        // BCrypt 匹配：明文 vs 密文
        boolean match = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!match) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "账号或密码错誤");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "登录成功");
        result.put("userId", user.getId());
        result.put("role", user.getRole());
        result.put("name", user.getName());
        result.put("username", user.getUsername());
        return result;
    }

    @Override
    public Map<String, Object> loginByPhone(LoginByPhoneRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错误");
            return result;
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "手机号不能为空");
            return result;
        }

        String normalizedPhone = ValidationUtil.normalizeCnMobile(request.getPhone());
        if (!ValidationUtil.isValidCnMobile(normalizedPhone)) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "手机号格式不合法");
            return result;
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "密码不能为空");
            return result;
        }

        User user = userMapper.findByPhone(normalizedPhone);
        if (user == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "手机号或密码错误");
            return result;
        }

        boolean match = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!match) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "手机号或密码错误");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "登录成功");
        result.put("userId", user.getId());
        result.put("role", user.getRole());
        result.put("name", user.getName());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        return result;
    }

    @Override
    public Map<String, Object> loginById(LoginByIdRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        if (request.getUserId() == null) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "用户ID不能为空");
            return result;
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "密码不能为空");
            return result;
        }

        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "账号或密码错誤");
            return result;
        }

        boolean match = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!match) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "账号或密码错誤");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "登录成功");
        result.put("userId", user.getId());
        result.put("role", user.getRole());
        result.put("name", user.getName());
        result.put("username", user.getUsername());
        return result;
    }

    @Override
    public Map<String, Object> changePassword(ChangePasswordRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (request == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        if (request.getUserId() == null) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "用户ID不能为空");
            return result;
        }

        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "旧密码不能为空");
            return result;
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "新密码不能为空");
            return result;
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "请再次输入新密码");
            return result;
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            result.put("success", false);
            result.put("code", ApiCode.VALIDATION_ERROR.getCode());
            result.put("message", "两次输入的密码不一致");
            return result;
        }

        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "用户不存在");
            return result;
        }

        boolean oldMatch = passwordEncoder.matches(request.getOldPassword(), user.getPassword());
        if (!oldMatch) {
            result.put("success", false);
            result.put("code", ApiCode.UNAUTHORIZED.getCode());
            result.put("message", "旧密码错誤");
            return result;
        }

        int updated = userMapper.updatePasswordById(user.getId(), passwordEncoder.encode(request.getNewPassword()));
        if (updated <= 0) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "修改失败");
            return result;
        }

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "修改成功");
        return result;
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "用户ID不能为空");
            return result;
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("code", ApiCode.NOT_FOUND.getCode());
            result.put("message", "用户不存在");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("phone", user.getPhone());
        data.put("age", user.getAge());
        data.put("sex", user.getSex());
        data.put("address", user.getAddress());
        data.put("riskLevel", user.getRiskLevel());

        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }
}



