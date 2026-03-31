package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "用户", description = "用户相关接口")
/**
 * 用户相关接口。
 */
public class UserContorller {
    @Resource
    private UserService userService;

    @GetMapping
    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    public List<User> getuser(){
        return userService.findAll();
    }

    /**
     * 查询所有患者的总人数（role = PATIENT）
     * 前端拿到的就是一個数字
     */
    @GetMapping("/patient/count")
    public Map<String, Object> getPatientCount() {
        Map<String, Object> result = new HashMap<>();
        Long count = userService.countPatients();
        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", count);
        return result;
    }

    @GetMapping("/patient/risk-stats")
    /**
     * 查询患者风险等级统计。
     *
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> getPatientRiskStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", userService.patientRiskStats());
        return result;
    }

    @GetMapping("/patient/{id}")
    @Operation(
            summary = "医生按患者ID查询患者信息",
            description = "鉴权规则：doctorUsername 必须存在且角色为 DOCTOR，并且 doctorPassword 与数据库密码匹配。\n\n"
                    + "查询规则：仅可查询 role=PATIENT 的用户；id 为 user 表主键（全局唯一）。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(value = "{\"success\":true,\"code\":200,\"message\":\"查询成功\",\"data\":{\"id\":123,\"username\":\"patient001\",\"name\":\"张三\",\"phone\":\"13800000000\",\"role\":\"PATIENT\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "无权限/鉴权失败",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"code\":40301,\"message\":\"無权限\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "患者不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"code\":40401,\"message\":\"患者不存在\"}")
                    )
            )
    })
    public Map<String, Object> getPatientById(@RequestParam String doctorUsername,
                                              @RequestParam String doctorPassword,
                                              @PathVariable Long id) {
        return userService.getPatientByIdForDoctor(doctorUsername, doctorPassword, id);
    }

    @GetMapping("/patient/search")
    @Operation(
            summary = "医生搜索患者列表",
            description = "鉴权规则：doctorUsername 必须存在且角色为 DOCTOR，并且 doctorPassword 与数据库密码匹配。\n\n"
                    + "keyword 可选：匹配 name/phone/username；limit 可选：默认 50，最大 200。"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(value = "{\"success\":true,\"code\":200,\"message\":\"查询成功\",\"data\":[{\"id\":123,\"name\":\"张三\",\"phone\":\"13800000000\",\"riskLevel\":\"中\"}]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "无权限/鉴权失败",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"code\":40301,\"message\":\"無权限\"}")
                    )
            )
    })
    public Map<String, Object> searchPatients(@RequestParam String doctorUsername,
                                              @RequestParam String doctorPassword,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer limit) {
        return userService.searchPatientsForDoctor(doctorUsername, doctorPassword, keyword, limit);
    }

}

