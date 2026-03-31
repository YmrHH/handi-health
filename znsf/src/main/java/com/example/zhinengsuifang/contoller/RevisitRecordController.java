package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.entity.RevisitRecord;
import com.example.zhinengsuifang.service.RevisitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/revisit", "/api/revisit"})
@Tag(name = "复诊记录", description = "复诊记录相关接口")
/**
 * 复诊记录相关接口。
 */
public class RevisitRecordController {

    @Resource
    private RevisitRecordService revisitRecordService;

    @PostMapping("/add")
    @Operation(
            summary = "医生新增患者复诊记录",
            description = "医生通过账号密码校验后，为指定患者新增一条复诊记录。\n\n规则：\n1) patientId、revisitAt 必填；\n2) revisitAt 只能填写当前时间或过去时间（不允许未来）；\n3) patientId 对应患者必须存在，且患者姓名（user.name）不能为空；\n4) doctorName/createdByDoctorId/createdAt 由后端写入或覆盖。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功/失败（success/code/message）", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "医生账号或密码错误"),
            @ApiResponse(responseCode = "403", description = "非医生角色/无权限"),
            @ApiResponse(responseCode = "404", description = "患者不存在"),
            @ApiResponse(responseCode = "422", description = "参数校验失败（如复诊时间为未来/医生账号密码为空/患者姓名为空等）")
    })
    /**
     * 医生新增患者复诊记录。
     *
     * @param record 复诊记录
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> add(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "复诊记录", content = @Content(schema = @Schema(implementation = RevisitRecord.class)))
            @Valid @RequestBody RevisitRecord record,
            @Parameter(description = "医生账号", required = true)
            @RequestParam String doctorUsername,
            @Parameter(description = "医生密码", required = true)
            @RequestParam String doctorPassword) {
        return revisitRecordService.addRevisitRecord(record, doctorUsername, doctorPassword);
    }
}

