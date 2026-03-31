package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.entity.HealthMetric;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康指标相关業务接口。
 */
public interface HealthMetricService {

    /**
     * 新增健康指标数据。
     *
     * @param metric 指标数据
     * @return 统一返返结构：success/message
     */
    Map<String, Object> addMetric(HealthMetric metric);

    /**
     * 医生查询患者最新指标列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientId 患者 ID
     * @param limit 條数上限（可選）
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> doctorLatestMetrics(String doctorUsername, String doctorPassword, Long patientId, Integer limit);

    /**
     * 医生按條件查询患者指标。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientId 患者 ID
     * @param metricType 指标類型（可選）
     * @param startAt 起始時間
     * @param endAt 结束時間
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> doctorQueryMetrics(String doctorUsername,
                                          String doctorPassword,
                                          Long patientId,
                                          String metricType,
                                          LocalDateTime startAt,
                                          LocalDateTime endAt);
}

