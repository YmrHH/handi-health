package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.dto.ChangePasswordRequest;
import com.example.zhinengsuifang.dto.CreateUserByDoctorRequest;
import com.example.zhinengsuifang.dto.LoginByIdRequest;
import com.example.zhinengsuifang.dto.LoginByPhoneRequest;
import com.example.zhinengsuifang.dto.LoginRequest;
import com.example.zhinengsuifang.dto.RegisterRequest;
import com.example.zhinengsuifang.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录 / 注册 控制器
 * 医生在 Web 端可以通过注册接口为患者、随访员创建账号；
 * 患者 / 随访员 / 医生通过登录接口登录（用 role 区分）。
 */
@Tag(name = "认证", description = "注册与登录相关接口")
@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * Web 端注册医生账号。
     * 请求体：username/password/confirmPassword/phone。
     * 后端会自动将 role 固定为 DOCTOR。
     */
    @PostMapping("/register")
    @Operation(
            summary = "Web 端注册医生账号",
            description = "用于 Web 端创建医生账号。\n\n"
                    + "说明：\n"
                    + "1) username 为登录账号（全系统唯一）；\n"
                    + "2) phone 为必填（Web 端医生使用手机号 + 密码登录）；\n"
                    + "3) 注册成功后 role 固定为 DOCTOR；\n"
                    + "4) 注册成功后会返回 userId（数据库自增主键），小程序端后续可使用 userId 进行登录/修改密码。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "注册成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"注册成功\",\"userId\":123,\"username\":\"doctor001\",\"role\":\"DOCTOR\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "参数校验失败/冲突",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = {
                                    @ExampleObject(name = "username_required", value = "{\"success\":false,\"code\":42201,\"message\":\"用户名不能为空\"}"),
                                    @ExampleObject(name = "password_mismatch", value = "{\"success\":false,\"code\":42201,\"message\":\"两次输入的密码不一致\"}"),
                                    @ExampleObject(name = "conflict", value = "{\"success\":false,\"code\":40901,\"message\":\"用户名已存在\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "资源冲突（如用户名/手机号已存在）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "conflict", value = "{\"success\":false,\"code\":40901,\"message\":\"用户名已存在\"}")
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return wrap(authService.register(request));
    }

    @PostMapping("/createUserByDoctor")
    @Operation(
            summary = "医生创建新用户（患者/随访员）",
            description = "用于医生在 Web 端创建新账号。一次请求仅创建一个账号，新账号角色只能是患者（PATIENT）或随访员（FOLLOW_UP）。\n\n"
                    + "鉴权规则（后端通过账号密码二次校验，不依赖登录态）：\n"
                    + "1) doctorUsername 必须存在；\n"
                    + "2) doctorUsername 对应用户的 role 必须为 DOCTOR；\n"
                    + "3) doctorPassword 为明文入参，后端用 BCrypt 与数据库密文密码进行 matches 校验。\n\n"
                    + "请求参数说明：\n"
                    + "- doctorUsername：医生账号（必填）\n"
                    + "- doctorPassword：医生密码明文（必填）\n"
                    + "- username：新账号登录名（必填、全系统唯一）\n"
                    + "- name：姓名/显示名（必填）\n"
                    + "- phone：手机号（必填）\n"
                    + "- role：新账号角色（必填，仅允许 PATIENT / FOLLOW_UP，不区分大小写，后端会转为大写保存）\n"
                    + "- riskLevel：风险等级（当 role=PATIENT 时必填，仅允许 低/中/高/上；role=FOLLOW_UP 时不需要）\n"
                    + "- address/age/sex：可选补充信息。\n\n"
                    + "落库与默认值：\n"
                    + "1) 新用户 status 固定为 1（启用）；\n"
                    + "2) 新用户默认初始密码为 123456（后端 BCrypt 加密后入库）；\n"
                    + "3) 创建成功后返回 userId 与 username。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "创建成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"创建成功\",\"userId\":123,\"username\":\"patient001\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "参数/校验失败（如账号为空、姓名为空、手机号为空、角色不合法、风险等级缺失/不合法等）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = {
                                    @ExampleObject(name = "name_required", value = "{\"success\":false,\"code\":42201,\"message\":\"姓名不能为空\"}"),
                                    @ExampleObject(name = "role_invalid", value = "{\"success\":false,\"code\":42201,\"message\":\"仅允许创建患者或随访员\"}"),
                                    @ExampleObject(name = "risk_level_required", value = "{\"success\":false,\"code\":42201,\"message\":\"风险等级不能为空\"}"),
                                    @ExampleObject(name = "risk_level_invalid", value = "{\"success\":false,\"code\":42201,\"message\":\"风险等级只能为：低/中/高/上\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "无权限/鉴权失败（医生账号不是 DOCTOR 或密码不匹配）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"医生账号或密码错誤\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权限（doctorUsername 对应用户不是 DOCTOR）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "forbidden", value = "{\"success\":false,\"code\":40301,\"message\":\"無权限\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "账号冲突（username 已存在）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "conflict", value = "{\"success\":false,\"code\":40901,\"message\":\"账号已存在\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "内部错误（创建后未获取到 userId 等异常情况）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "internal_error", value = "{\"success\":false,\"code\":50000,\"message\":\"创建失败\"}")
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "创建用户请求体",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateUserByDoctorRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "create_patient",
                                    value = "{\"doctorUsername\":\"doctor001\",\"doctorPassword\":\"123456\",\"username\":\"patient001\",\"name\":\"张三\",\"phone\":\"13800000000\",\"role\":\"PATIENT\",\"riskLevel\":\"中\",\"address\":\"广东省深圳市\",\"age\":45,\"sex\":\"男\"}"
                            ),
                            @ExampleObject(
                                    name = "create_follow_up",
                                    value = "{\"doctorUsername\":\"doctor001\",\"doctorPassword\":\"123456\",\"username\":\"followup001\",\"name\":\"李四\",\"phone\":\"13900000000\",\"role\":\"FOLLOW_UP\",\"address\":\"广东省深圳市\"}"
                            )
                    }
            )
    )
    public ResponseEntity<Map<String, Object>> createUserByDoctor(@RequestBody CreateUserByDoctorRequest request) {
        return wrap(authService.createUserByDoctor(request));
    }

    /**
     * 登录接口
     * 请求体：{"username": "...", "password": "..."}
     * 返返：success, message, role, userId 等
     */
    @PostMapping("/login")
    @Operation(
            summary = "账号密码登录（基于 username）",
            description = "通用登录接口：使用 username + password 登录。返回 userId/role/name。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "登录成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"登录成功\",\"userId\":123,\"role\":\"DOCTOR\",\"name\":\"张三\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "账号或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"账号或密码错誤\"}")
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        return wrap(authService.login(loginRequest.getUsername(), loginRequest.getPassword()));
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return wrap(java.util.Map.of(
                "success", true,
                "code", ApiCode.SUCCESS.getCode(),
                "message", "已退出登录"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutPost() {
        return logout();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public ResponseEntity<Map<String, Object>> me(jakarta.servlet.http.HttpServletRequest request) {
        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        if (userId == null) {
            return wrap(java.util.Map.of(
                    "success", false,
                    "code", ApiCode.UNAUTHORIZED.getCode(),
                    "message", "未登录或登录已过期"
            ));
        }
        return wrap(authService.getUserInfo(userId));
    }

    @PostMapping("/loginByPhone")
    @Operation(
            summary = "Web 端医生手机号登录",
            description = "Web 端医生登录接口：使用 phone + password 登录。\n\n"
                    + "规则：\n"
                    + "1) phone 必须存在且对应用户角色必须为 DOCTOR；\n"
                    + "2) password 为明文入参，后端使用 BCrypt 与数据库密文进行 matches 校验；\n"
                    + "3) 登录成功返回 userId（用于小程序端登录/改密）与 role/name。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "登录成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"登录成功\",\"userId\":123,\"role\":\"DOCTOR\",\"name\":\"张三\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "参数校验失败/无权限/账号或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = {
                                    @ExampleObject(name = "phone_required", value = "{\"success\":false,\"code\":42201,\"message\":\"手机号不能为空\"}"),
                                    @ExampleObject(name = "password_required", value = "{\"success\":false,\"code\":42201,\"message\":\"密码不能为空\"}"),
                                    @ExampleObject(name = "forbidden", value = "{\"success\":false,\"code\":40301,\"message\":\"無权限\"}"),
                                    @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"账号或密码错誤\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "账号或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"账号或密码错誤\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权限（非医生账号）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "forbidden", value = "{\"success\":false,\"code\":40301,\"message\":\"無权限\"}")
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Web 端医生手机号登录请求体",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginByPhoneRequest.class),
                    examples = @ExampleObject(name = "login_by_phone", value = "{\"phone\":\"13800000000\",\"password\":\"123456\"}")
            )
    )
    public ResponseEntity<Map<String, Object>> loginByPhone(@RequestBody LoginByPhoneRequest request) {
        return wrap(authService.loginByPhone(request));
    }

    @PostMapping("/loginById")
    @Operation(
            summary = "小程序端登录（基于 userId）",
            description = "小程序端登录推荐使用 userId + password。\n\n"
                    + "说明：\n"
                    + "1) userId 来自医生注册接口返回（或医生创建患者/随访员后查询到的 userId）；\n"
                    + "2) password 为明文入参，后端用 BCrypt 与数据库密文密码进行 matches 校验。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "登录成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"登录成功\",\"userId\":123,\"role\":\"PATIENT\",\"name\":\"张三\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "参数校验失败/账号或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = {
                                    @ExampleObject(name = "userId_required", value = "{\"success\":false,\"code\":42201,\"message\":\"用户ID不能为空\"}"),
                                    @ExampleObject(name = "password_required", value = "{\"success\":false,\"code\":42201,\"message\":\"密码不能为空\"}"),
                                    @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"账号或密码错誤\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "账号或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"账号或密码错誤\"}")
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "小程序端基于 userId 登录请求体",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginByIdRequest.class),
                    examples = @ExampleObject(name = "login_by_id", value = "{\"userId\":123,\"password\":\"123456\"}")
            )
    )
    public ResponseEntity<Map<String, Object>> loginById(@RequestBody LoginByIdRequest request) {
        return wrap(authService.loginById(request));
    }

    @PostMapping("/changePassword")
    @Operation(
            summary = "小程序端修改密码（校验旧密码）",
            description = "小程序端修改密码接口。\n\n"
                    + "规则：\n"
                    + "1) 必须提供 userId、oldPassword、newPassword、confirmPassword；\n"
                    + "2) oldPassword 必须与数据库密码匹配（BCrypt matches）；\n"
                    + "3) newPassword 与 confirmPassword 必须一致。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "修改成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"修改成功\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "参数校验失败/用户不存在/旧密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = {
                                    @ExampleObject(name = "password_mismatch", value = "{\"success\":false,\"code\":42201,\"message\":\"两次输入的密码不一致\"}"),
                                    @ExampleObject(name = "user_not_found", value = "{\"success\":false,\"code\":40401,\"message\":\"用户不存在\"}"),
                                    @ExampleObject(name = "old_password_wrong", value = "{\"success\":false,\"code\":40101,\"message\":\"旧密码错誤\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "旧密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "old_password_wrong", value = "{\"success\":false,\"code\":40101,\"message\":\"旧密码错誤\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "用户不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "user_not_found", value = "{\"success\":false,\"code\":40401,\"message\":\"用户不存在\"}")
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "小程序端修改密码请求体",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChangePasswordRequest.class),
                    examples = @ExampleObject(name = "change_password", value = "{\"userId\":123,\"oldPassword\":\"123456\",\"newPassword\":\"abc12345\",\"confirmPassword\":\"abc12345\"}")
            )
    )
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        return wrap(authService.changePassword(request));
    }

    private ResponseEntity<Map<String, Object>> wrap(Map<String, Object> body) {
        if (body == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        Object codeObj = body.get("code");
        Integer code = null;
        if (codeObj instanceof Integer) {
            code = (Integer) codeObj;
        } else if (codeObj instanceof Number) {
            code = ((Number) codeObj).intValue();
        }

        HttpStatus status = mapCodeToStatus(code);
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus mapCodeToStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (code.equals(ApiCode.SUCCESS.getCode())) {
            return HttpStatus.OK;
        }

        if (code.equals(ApiCode.UNAUTHORIZED.getCode())) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (code.equals(ApiCode.FORBIDDEN.getCode())) {
            return HttpStatus.FORBIDDEN;
        }

        if (code.equals(ApiCode.NOT_FOUND.getCode())) {
            return HttpStatus.NOT_FOUND;
        }

        if (code.equals(ApiCode.CONFLICT.getCode())) {
            return HttpStatus.CONFLICT;
        }

        if (code.equals(ApiCode.VALIDATION_ERROR.getCode())) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }

        if (code.equals(ApiCode.PARAM_ERROR.getCode())) {
            return HttpStatus.BAD_REQUEST;
        }

        if (code.equals(ApiCode.INTERNAL_ERROR.getCode())) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return HttpStatus.BAD_REQUEST;
    }
}

