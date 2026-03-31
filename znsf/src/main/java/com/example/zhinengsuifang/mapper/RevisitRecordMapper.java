package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.RevisitRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 复诊记录表相关 Mapper。
 */
public interface RevisitRecordMapper {

    @Insert("INSERT INTO revisit_record (patient_id, revisit_at, hospital, department, doctor_name, notes, created_by_doctor_id, created_at) " +
            "VALUES (#{patientId}, #{revisitAt}, #{hospital}, #{department}, #{doctorName}, #{notes}, #{createdByDoctorId}, NOW())")
    /**
     * 新增复诊记录。
     */
    void insert(RevisitRecord record);

    @Select("select * from revisit_record where patient_id = #{patientId} order by revisit_at desc limit 1")
    /**
     * 查询患者最新一條复诊记录。
     */
    RevisitRecord findLatestByPatientId(@Param("patientId") Long patientId);

    @Select("select * from revisit_record where patient_id = #{patientId} order by revisit_at desc")
    /**
     * 查询患者的复诊记录列表。
     */
    List<RevisitRecord> findByPatientId(@Param("patientId") Long patientId);
}

