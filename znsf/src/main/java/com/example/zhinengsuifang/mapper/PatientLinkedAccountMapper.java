package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientLinkedAccount;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PatientLinkedAccountMapper {

    @Select("select id, patient_id as patientId, linked_user_id as linkedUserId, relation, linked_username as linkedUsername, linked_phone as linkedPhone, status, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_linked_account where patient_id=#{patientId} order by id desc")
    List<PatientLinkedAccount> findByPatientId(@Param("patientId") Long patientId);

    @Insert("insert into patient_linked_account (patient_id, linked_user_id, relation, linked_username, linked_phone, status, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{linkedUserId}, #{relation}, #{linkedUsername}, #{linkedPhone}, #{status}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientLinkedAccount a);

    @Delete("delete from patient_linked_account where id=#{id} and patient_id=#{patientId}")
    int deleteById(@Param("patientId") Long patientId, @Param("id") Long id);
}
