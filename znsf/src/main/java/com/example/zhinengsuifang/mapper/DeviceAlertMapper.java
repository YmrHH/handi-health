package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.DeviceAlert;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DeviceAlertMapper {

    @Insert("INSERT INTO device_alert (patient_id, device_sn, alert_code, alert_message, severity, status, occurred_at, created_at) " +
            "VALUES (#{patientId}, #{deviceSn}, #{alertCode}, #{alertMessage}, #{severity}, #{status}, #{occurredAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DeviceAlert alert);

    @Select("select * from device_alert where id = #{id}")
    DeviceAlert findById(@Param("id") Long id);

    @Select("select * from device_alert order by created_at desc")
    List<DeviceAlert> findAll();

    @Update("update device_alert set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
