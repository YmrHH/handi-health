package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientLabResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PatientLabResultMapper {

    @Insert("insert into patient_lab_result (patient_id, lab_type, value_text, value_num, unit, status, measured_at, hospital, remark, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{labType}, #{valueText}, #{valueNum}, #{unit}, #{status}, #{measuredAt}, #{hospital}, #{remark}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientLabResult r);

    @Select("select id, patient_id as patientId, lab_type as labType, value_text as valueText, value_num as valueNum, unit, status, measured_at as measuredAt, hospital, remark, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_lab_result where patient_id=#{patientId} order by measured_at desc, id desc")
    List<PatientLabResult> findByPatientId(@Param("patientId") Long patientId);
}
