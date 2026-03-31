package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.dto.HealthAlertWithPatient;
import com.example.zhinengsuifang.entity.HealthAlert;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 健康预警表相关 Mapper。
 */
public interface HealthAlertMapper {

    @Insert("INSERT INTO health_alert (patient_id, metric_type, prev_value1, prev_value2, curr_value1, curr_value2, delta_value1, delta_value2, severity, status, created_at) " +
            "VALUES (#{patientId}, #{metricType}, #{prevValue1}, #{prevValue2}, #{currValue1}, #{currValue2}, #{deltaValue1}, #{deltaValue2}, #{severity}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /**
     * 新增预警。
     */
    void insert(HealthAlert alert);

    @Select("select * from health_alert order by created_at desc")
    /**
     * 查询全部预警（按创建時間倒序）。
     */
    List<HealthAlert> findAll();

    @Select("select * from health_alert where status = #{status} order by created_at desc")
    /**
     * 按状态查询预警。
     */
    List<HealthAlert> findByStatus(@Param("status") String status);

    @Select("select * from health_alert where id = #{id}")
    /**
     * 按 id 查询预警。
     */
    HealthAlert findById(@Param("id") Long id);

    @Update("update health_alert set status = #{status} where id = #{id}")
    /**
     * 更新预警状态。
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("select a.id as id, a.patient_id as patientId, u.name as patientName, u.phone as patientPhone, " +
            "a.metric_type as metricType, a.prev_value1 as prevValue1, a.prev_value2 as prevValue2, " +
            "a.curr_value1 as currValue1, a.curr_value2 as currValue2, a.delta_value1 as deltaValue1, a.delta_value2 as deltaValue2, " +
            "a.severity as severity, a.status as status, a.created_at as createdAt " +
            "from health_alert a join user u on u.id = a.patient_id " +
            "where u.role = 'PATIENT' " +
            "and (#{patientName} is null or #{patientName} = '' or u.name like concat('%', #{patientName}, '%')) " +
            "and (#{status} is null or #{status} = '' or a.status = #{status}) " +
            "order by a.created_at desc limit #{limit}")
    List<HealthAlertWithPatient> findAlertsByPatientName(@Param("patientName") String patientName,
                                                        @Param("status") String status,
                                                        @Param("limit") Integer limit);
}
