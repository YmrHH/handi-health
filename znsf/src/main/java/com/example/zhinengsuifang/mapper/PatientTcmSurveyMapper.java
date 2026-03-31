package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientTcmSurvey;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientTcmSurveyMapper {

    @Insert("insert into patient_tcm_survey (patient_id, survey_type, answers_json, result_json, assessed_at, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{surveyType}, #{answersJson}, #{resultJson}, #{assessedAt}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientTcmSurvey s);

    @Select("select id, patient_id as patientId, survey_type as surveyType, cast(answers_json as char) as answersJson, cast(result_json as char) as resultJson, assessed_at as assessedAt, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_tcm_survey where patient_id=#{patientId} order by assessed_at desc")
    List<PatientTcmSurvey> findByPatientId(@Param("patientId") Long patientId);

    @Select("select count(*) from patient_tcm_survey where patient_id=#{patientId} and assessed_at >= #{startAt} and assessed_at < #{endAt}")
    Long countTodayByPatientId(@Param("patientId") Long patientId,
                               @Param("startAt") LocalDateTime startAt,
                               @Param("endAt") LocalDateTime endAt);

}
