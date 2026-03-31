package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientFeedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PatientFeedbackMapper {

    @Insert("insert into patient_feedback (patient_id, feedback_type, content, contact, status, reply, replied_at, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{feedbackType}, #{content}, #{contact}, #{status}, #{reply}, #{repliedAt}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientFeedback f);

    @Select("select id, patient_id as patientId, feedback_type as feedbackType, content, contact, status, reply, replied_at as repliedAt, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_feedback where (#{patientId} is null or patient_id=#{patientId}) order by created_at desc, id desc")
    List<PatientFeedback> findByPatientId(@Param("patientId") Long patientId);
}
