package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.dto.CreateFollowUpFromAlertRequest;
import com.example.zhinengsuifang.dto.MarkHealthAlertRequest;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.HealthAlertService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/alert")
@Tag(name = "健康预警", description = "健康预警相关接口")
/**
 * 健康预警相关接口。
 * <p>
 * 包括：医生查询预警列表、基於预警生成随访单、标记预警处理状态等。
 * </p>
 */
public class HealthAlertController {

    @Resource
    private HealthAlertService healthAlertService;

    @Resource
    private UserMapper userMapper;

    private Long currentUserId(HttpServletRequest request) {
        return com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
    }

    private Long requireDoctorId(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return null;
        }
        User u = userMapper.findById(userId);
        if (u == null || u.getRole() == null) {
            return null;
        }
        if (!"DOCTOR".equalsIgnoreCase(u.getRole().trim())) {
            return null;
        }
        return u.getId();
    }

    @GetMapping("/list")
    /**
     * 查询预警列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param status 可選状态过濾
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(required = false) String doctorUsername,
                                    @RequestParam(required = false) String doctorPassword,
                                    @RequestParam(required = false) String status) {
        Long doctorId = requireDoctorId(request);
        if (doctorId != null) {
            return healthAlertService.listAlertsByDoctorId(doctorId, status);
        }
        return healthAlertService.listAlerts(doctorUsername, doctorPassword, status);
    }

    @GetMapping("/searchByPatientName")
    public Map<String, Object> searchByPatientName(HttpServletRequest request,
                                                   @RequestParam(required = false) String doctorUsername,
                                                   @RequestParam(required = false) String doctorPassword,
                                                   @RequestParam(required = false) String patientName,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Integer limit) {
        Long doctorId = requireDoctorId(request);
        if (doctorId != null) {
            return healthAlertService.searchAlertsByPatientNameByDoctorId(doctorId, patientName, status, limit);
        }
        return healthAlertService.searchAlertsByPatientName(doctorUsername, doctorPassword, patientName, status, limit);
    }

    @PostMapping("/createFollowUp")
    /**
     * 根据健康预警生成随访单。
     *
     * @param request 生成随访单请求
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> createFollowUp(HttpServletRequest httpServletRequest, @RequestBody CreateFollowUpFromAlertRequest request) {
        Long doctorId = requireDoctorId(httpServletRequest);
        if (doctorId != null) {
            return healthAlertService.createFollowUpFromAlertByDoctorId(
                    doctorId,
                    request == null ? null : request.getAlertId(),
                    request == null ? null : request.getFollowUpUserId(),
                    request == null ? null : request.getDescription(),
                    request == null ? null : request.getDueAt()
            );
        }
        return healthAlertService.createFollowUpFromAlert(request);
    }

    @PostMapping("/mark")
    /**
     * 医生标记预警状态（例如 REVIEWED/IGNORED）。
     *
     * @param request 标记请求
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> mark(HttpServletRequest httpServletRequest, @RequestBody MarkHealthAlertRequest request) {
        Long doctorId = requireDoctorId(httpServletRequest);
        if (doctorId != null) {
            return healthAlertService.markAlertByDoctorId(
                    doctorId,
                    request == null ? null : request.getAlertId(),
                    request == null ? null : request.getStatus()
            );
        }
        return healthAlertService.markAlert(request);
    }

    @GetMapping("/form")
    public Map<String, Object> form(HttpServletRequest request,
                                    @RequestParam(required = false) String doctorUsername,
                                    @RequestParam(required = false) String doctorPassword,
                                    @RequestParam Long alertId) {
        Long doctorId = requireDoctorId(request);
        if (doctorId != null) {
            return healthAlertService.getAlertFormByDoctorId(doctorId, alertId);
        }
        return healthAlertService.getAlertForm(doctorUsername, doctorPassword, alertId);
    }
}

