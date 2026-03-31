package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientFollowupApplication;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PatientFollowupApplicationMapper {

    @Insert("insert into patient_followup_application (patient_id, apply_reason, preferred_time, contact_phone, status, handled_by_user_id, handled_at, handle_note, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{applyReason}, #{preferredTime}, #{contactPhone}, #{status}, #{handledByUserId}, #{handledAt}, #{handleNote}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientFollowupApplication a);

    @Select("select id, patient_id as patientId, apply_reason as applyReason, preferred_time as preferredTime, contact_phone as contactPhone, status, handled_by_user_id as handledByUserId, handled_at as handledAt, handle_note as handleNote, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_followup_application where patient_id=#{patientId} order by created_at desc, id desc")
    List<PatientFollowupApplication> findByPatientId(@Param("patientId") Long patientId);
}
