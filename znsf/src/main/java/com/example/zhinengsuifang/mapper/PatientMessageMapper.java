package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PatientMessageMapper {

    @Insert("insert into patient_message (patient_id, title, content, message_type, status, read_at, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) " +
            "values (#{patientId}, #{title}, #{content}, #{messageType}, #{status}, #{readAt}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientMessage message);

    @Select("select id, patient_id as patientId, title, content, message_type as messageType, status, read_at as readAt, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_message where patient_id=#{patientId} order by created_at desc, id desc")
    List<PatientMessage> findByPatientId(@Param("patientId") Long patientId);

    @Select("select id, patient_id as patientId, title, content, message_type as messageType, status, read_at as readAt, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_message where patient_id=#{patientId} order by created_at desc, id desc limit 1")
    PatientMessage findLatestByPatientId(@Param("patientId") Long patientId);

    @Select("select count(*) from patient_message where patient_id=#{patientId} and status<>'READ'")
    Integer countUnreadByPatientId(@Param("patientId") Long patientId);

    @Update("update patient_message set status='READ', read_at=now(), updated_at=now() where id=#{id} and patient_id=#{patientId}")
    int markRead(@Param("patientId") Long patientId, @Param("id") Long id);

    @Update("update patient_message set status='READ', read_at=now(), updated_at=now() where patient_id=#{patientId} and status<>'READ'")
    int markAllRead(@Param("patientId") Long patientId);
}
