package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.service.CandidateService;
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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/candidate")
@Tag(name = "候选患者", description = "候选患者相关接口")
/**
 * 候選患者相关接口。
 * <p>
 * 主要提供医生端查询：基於異常指标/長期未复诊等規則，生成需要重點随访的候選患者列表。
 * </p>
 */
public class CandidateController {

    @Resource
    private CandidateService candidateService;

    @GetMapping("/patients")
    @Operation(
            summary = "查询候选患者列表（医生端）",
            description = "医生端根据异常指标/长期未复诊等规则，生成需要重点随访的候选患者列表。\n\n"
                    + "鉴权：doctorUsername 必须为医生账号（role=DOCTOR），doctorPassword 为明文入参，后端使用 BCrypt matches 校验。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "success", value = "{\"success\":true,\"code\":0,\"message\":\"查询成功\",\"data\":[]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "参数校验失败（医生账号或密码为空）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "validation_error", value = "{\"success\":false,\"code\":42201,\"message\":\"医生账号或密码不能为空\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未授权（医生账号或密码错误）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "unauthorized", value = "{\"success\":false,\"code\":40101,\"message\":\"医生账号或密码错誤\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权限（doctorUsername 不是医生账号）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "forbidden", value = "{\"success\":false,\"code\":40301,\"message\":\"無权限\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "系统异常",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(name = "internal_error", value = "{\"success\":false,\"code\":50000,\"message\":\"系统异常\"}")
                    )
            )
    })
    /**
     * 查询候選患者列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码（明文，后端使用 BCrypt matches 校驗）
     * @return 统一返返结构：success/message/data
     */
    public ResponseEntity<Map<String, Object>> patients(@RequestParam String doctorUsername, @RequestParam String doctorPassword) {
        return wrap(candidateService.candidatePatients(doctorUsername, doctorPassword));
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

        if (code.equals(ApiCode.VALIDATION_ERROR.getCode())) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }

        if (code.equals(ApiCode.PARAM_ERROR.getCode())) {
            return HttpStatus.BAD_REQUEST;
        }

        if (code.equals(ApiCode.NOT_FOUND.getCode())) {
            return HttpStatus.NOT_FOUND;
        }

        if (code.equals(ApiCode.CONFLICT.getCode())) {
            return HttpStatus.CONFLICT;
        }

        if (code.equals(ApiCode.INTERNAL_ERROR.getCode())) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return HttpStatus.BAD_REQUEST;
    }
}

