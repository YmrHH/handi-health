package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.entity.HealthMetric;
import com.example.zhinengsuifang.service.HealthMetricService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/healthMetric")
@Tag(name = "健康指标", description = "健康指标上报与查询接口")
/**
 * 健康指标（例如血壓）上報與查询接口。
 */
public class HealthMetricController {

    @Resource
    private HealthMetricService healthMetricService;

    @PostMapping("/add")
    /**
     * 新增一條健康指标数据。
     *
     * @param metric 健康指标
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> add(@RequestBody HealthMetric metric) {
        return healthMetricService.addMetric(metric);
    }

    @GetMapping("/doctor/patient/latest")
    /**
     * 医生查询某患者最新的健康指标数据。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientId 患者 ID
     * @param limit 返返條数上限（可選）
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> doctorPatientLatest(@RequestParam String doctorUsername,
                                                   @RequestParam String doctorPassword,
                                                   @RequestParam Long patientId,
                                                   @RequestParam(required = false) Integer limit) {
        return healthMetricService.doctorLatestMetrics(doctorUsername, doctorPassword, patientId, limit);
    }

    @GetMapping("/doctor/patient/query")
    /**
     * 医生按時間範圍查询患者健康指标。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientId 患者 ID
     * @param metricType 指标類型（可選）
     * @param startAt 起始時間
     * @param endAt 结束時間
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> doctorPatientQuery(@RequestParam String doctorUsername,
                                                  @RequestParam String doctorPassword,
                                                  @RequestParam Long patientId,
                                                  @RequestParam(required = false) String metricType,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {
        return healthMetricService.doctorQueryMetrics(doctorUsername, doctorPassword, patientId, metricType, startAt, endAt);
    }
}

