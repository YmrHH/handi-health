package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientDailyMeasurement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientDailyMeasurementMapper {

    @Insert("insert into patient_daily_measurement (patient_id, measured_at, sbp, dbp, heart_rate, weight_kg, temperature_c, spo2, glucose, glucose_type, sleep_hours, symptoms, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) " +
            "values (#{patientId}, #{measuredAt}, #{sbp}, #{dbp}, #{heartRate}, #{weightKg}, #{temperatureC}, #{spo2}, #{glucose}, #{glucoseType}, #{sleep}, #{symptoms}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientDailyMeasurement m);

    @Select("select id, patient_id as patientId, measured_at as measuredAt, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, temperature_c as temperatureC, spo2, glucose as glucose, glucose_type as glucoseType, sleep_hours as sleep, symptoms, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 " +
            "from patient_daily_measurement where patient_id=#{patientId} and measured_at >= #{startAt} and measured_at < #{endAt} order by measured_at desc")
    List<PatientDailyMeasurement> findInRange(@Param("patientId") Long patientId,
                                              @Param("startAt") LocalDateTime startAt,
                                              @Param("endAt") LocalDateTime endAt);

    @Select("select id, patient_id as patientId, measured_at as measuredAt, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, temperature_c as temperatureC, spo2, glucose as glucose, glucose_type as glucoseType, sleep_hours as sleep, symptoms, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_daily_measurement where patient_id=#{patientId} order by measured_at desc limit 1")
    PatientDailyMeasurement findLatest(@Param("patientId") Long patientId);

    @Select("select id, patient_id as patientId, measured_at as measuredAt, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, temperature_c as temperatureC, spo2, glucose as glucose, glucose_type as glucoseType, sleep_hours as sleep, symptoms, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 " +
            "from patient_daily_measurement where patient_id=#{patientId} order by measured_at desc limit #{limit}")
    List<PatientDailyMeasurement> findRecent(@Param("patientId") Long patientId,
                                             @Param("limit") Integer limit);

    @Select("select count(*) from patient_daily_measurement where patient_id=#{patientId} and measured_at >= #{startAt} and measured_at < #{endAt}")
    Long countTodayByPatientId(@Param("patientId") Long patientId,
                               @Param("startAt") LocalDateTime startAt,
                               @Param("endAt") LocalDateTime endAt);

}
