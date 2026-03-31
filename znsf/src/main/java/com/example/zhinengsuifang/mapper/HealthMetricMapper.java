package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.HealthMetric;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康指标表相关 Mapper。
 */
public interface HealthMetricMapper {

    @Insert("INSERT INTO health_metric (patient_id, metric_type, value1, value2, measured_at, created_by_user_id, created_at) " +
            "VALUES (#{patientId}, #{metricType}, #{value1}, #{value2}, #{measuredAt}, #{createdByUserId}, NOW())")
    /**
     * 新增健康指标。
     *
     * @param metric 指标
     */
    void insert(HealthMetric metric);

    @Select("select count(*) from health_metric where patient_id = #{patientId} and metric_type = 'BP' and measured_at >= #{startAt} and measured_at <= #{endAt} " +
            "and ((value1 >= 140 and value1 < 180) or (value2 >= 90 and value2 < 110))")
    /**
     * 统计指定時間範圍內「非極端血壓異常」次数。
     *
     * @param patientId 患者 id
     * @param startAt 起始時間
     * @param endAt 结束時間
     * @return 異常次数
     */
    Long countBpAbnormalNonExtreme(@Param("patientId") Long patientId,
                                  @Param("startAt") LocalDateTime startAt,
                                  @Param("endAt") LocalDateTime endAt);

    @Select("select count(distinct date(measured_at)) from health_metric " +
            "where patient_id = #{patientId} and metric_type = 'BP' " +
            "and measured_at >= #{startAt} and measured_at <= #{endAt} " +
            "and (value1 > 160 or value2 > 90)")
    Long countBpHighDistinctDays(@Param("patientId") Long patientId,
                                @Param("startAt") LocalDateTime startAt,
                                @Param("endAt") LocalDateTime endAt);

    @Select("select * from health_metric where patient_id = #{patientId} order by measured_at desc limit 50")
    /**
     * 查询患者最新 50 條指标。
     */
    List<HealthMetric> findLatestByPatientId(@Param("patientId") Long patientId);

    @Select("select * from health_metric where patient_id = #{patientId} order by measured_at desc limit #{limit}")
    /**
     * 查询患者最新 N 條指标。
     */
    List<HealthMetric> findLatestByPatientIdLimit(@Param("patientId") Long patientId, @Param("limit") Integer limit);

    @Select("select * from health_metric where patient_id = #{patientId} and (#{metricType} is null or metric_type = #{metricType}) and measured_at >= #{startAt} and measured_at <= #{endAt} order by measured_at desc")
    /**
     * 按患者/類型/時間範圍查询指标列表。
     */
    List<HealthMetric> findByPatientIdAndTypeAndRange(@Param("patientId") Long patientId,
                                                     @Param("metricType") String metricType,
                                                     @Param("startAt") LocalDateTime startAt,
                                                     @Param("endAt") LocalDateTime endAt);

    @Select("select * from health_metric where patient_id = #{patientId} and metric_type = #{metricType} order by measured_at desc limit 1")
    /**
     * 查询患者某類型指标最新一條。
     */
    HealthMetric findLatestByPatientIdAndType(@Param("patientId") Long patientId, @Param("metricType") String metricType);
}

