package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientBasicInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PatientBasicInfoMapper {

    @Select("select id, patient_id as patientId, height_cm as heightCm, baseline_weight_kg as baselineWeightKg, birthday, id_card as idCard, emergency_contact_name as emergencyContactName, emergency_contact_phone as emergencyContactPhone, insurance_type as insuranceType, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_basic_info where patient_id = #{patientId} limit 1")
    PatientBasicInfo findByPatientId(@Param("patientId") Long patientId);

    @Insert("insert into patient_basic_info (patient_id, height_cm, baseline_weight_kg, birthday, id_card, emergency_contact_name, emergency_contact_phone, insurance_type, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{heightCm}, #{baselineWeightKg}, #{birthday}, #{idCard}, #{emergencyContactName}, #{emergencyContactPhone}, #{insuranceType}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientBasicInfo info);

    @Update("update patient_basic_info set height_cm=#{heightCm}, baseline_weight_kg=#{baselineWeightKg}, birthday=#{birthday}, id_card=#{idCard}, emergency_contact_name=#{emergencyContactName}, emergency_contact_phone=#{emergencyContactPhone}, insurance_type=#{insuranceType}, updated_at=now(), ext1=#{ext1}, ext2=#{ext2}, ext3=#{ext3}, ext4=#{ext4}, ext5=#{ext5} where patient_id=#{patientId}")
    int updateByPatientId(PatientBasicInfo info);

    @Select("select distinct ext3 from patient_basic_info where ext3 is not null and trim(ext3) <> '' order by ext3")
    List<String> selectDistinctDiseases();

    @Select("select distinct ext4 from patient_basic_info where ext4 is not null and trim(ext4) <> '' order by ext4")
    List<String> selectDistinctSyndromes();
}
