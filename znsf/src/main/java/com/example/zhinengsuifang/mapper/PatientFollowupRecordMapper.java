package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientFollowupRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PatientFollowupRecordMapper {

    @Insert("insert into patient_followup_record (" +
            "patient_id, doctor_id, staff_id, followup_time, followup_type, result_status, risk_level, content_summary, symptom_change, " +
            "sbp, dbp, heart_rate, weight_kg, vital_measure_time, med_adherence, med_plan_summary, adverse_reaction, " +
            "tcm_face, tcm_tongue_body, tcm_tongue_coating, tcm_tongue_image_url, tcm_pulse_rate, tcm_pulse_types, tcm_conclusion, tcm_remark, " +
            "lab_summary, next_followup_type, next_followup_date, next_followup_remark, ext1, ext2, ext3, ext4, ext5, created_at, updated_at" +
            ") values (" +
            "#{patientId}, #{doctorId}, #{staffId}, #{followupTime}, #{followupType}, #{resultStatus}, #{riskLevel}, #{contentSummary}, #{symptomChange}, " +
            "#{sbp}, #{dbp}, #{heartRate}, #{weightKg}, #{vitalMeasureTime}, #{medAdherence}, #{medPlanSummary}, #{adverseReaction}, " +
            "#{tcmFace}, #{tcmTongueBody}, #{tcmTongueCoating}, #{tcmTongueImageUrl}, #{tcmPulseRate}, #{tcmPulseTypes}, #{tcmConclusion}, #{tcmRemark}, " +
            "#{labSummary}, #{nextFollowupType}, #{nextFollowupDate}, #{nextFollowupRemark}, #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5}, now(), now()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientFollowupRecord record);



    @Select("select " +
            "  r.id as id, " +
            "  r.patient_id as patientId, " +
            "  r.doctor_id as doctorId, " +
            "  r.staff_id as staffId, " +
            "  r.followup_time as followupTime, " +
            "  r.followup_type as followupType, " +
            "  r.result_status as resultStatus, " +
            "  r.risk_level as riskLevel, " +
            "  r.content_summary as contentSummary, " +
            "  r.symptom_change as symptomChange, " +
            "  r.sbp as sbp, " +
            "  r.dbp as dbp, " +
            "  r.heart_rate as heartRate, " +
            "  r.weight_kg as weightKg, " +
            "  r.vital_measure_time as vitalMeasureTime, " +
            "  r.med_adherence as medAdherence, " +
            "  r.med_plan_summary as medPlanSummary, " +
            "  r.adverse_reaction as adverseReaction, " +
            "  r.tcm_face as tcmFace, " +
            "  r.tcm_tongue_body as tcmTongueBody, " +
            "  r.tcm_tongue_coating as tcmTongueCoating, " +
            "  r.tcm_tongue_image_url as tcmTongueImageUrl, " +
            "  r.tcm_pulse_rate as tcmPulseRate, " +
            "  r.tcm_pulse_types as tcmPulseTypes, " +
            "  r.tcm_conclusion as tcmConclusion, " +
            "  r.tcm_remark as tcmRemark, " +
            "  r.lab_summary as labSummary, " +
            "  r.next_followup_type as nextFollowupType, " +
            "  r.next_followup_date as nextFollowupDate, " +
            "  r.next_followup_remark as nextFollowupRemark, " +
            "  r.created_at as createdAt, " +
            "  r.updated_at as updatedAt, " +
            "  r.ext1 as ext1, " +
            "  r.ext2 as ext2, " +
            "  r.ext3 as ext3, " +
            "  r.ext4 as ext4, " +
            "  r.ext5 as ext5, " +
            "  s.name as staffName, " +
            "  d.name as doctorName, " +
            "  case " +
            "    when s.name is not null and trim(s.name) <> '' then s.name " +
            "    when d.name is not null and trim(d.name) <> '' then d.name " +
            "    else '医生/护士' " +
            "  end as followupStaffName " +
            "from patient_followup_record r " +
            "left join user s on s.id = r.staff_id " +
            "left join user d on d.id = r.doctor_id " +
            "where r.patient_id = #{patientId} " +
            "order by r.followup_time desc, r.id desc")
    List<Map<String, Object>> findPatientListByPatientId(@Param("patientId") Long patientId);

    @Select("select " +
            "  r.id as id, " +
            "  r.patient_id as patientId, " +
            "  r.doctor_id as doctorId, " +
            "  r.staff_id as staffId, " +
            "  r.followup_time as followupTime, " +
            "  r.followup_type as followupType, " +
            "  r.result_status as resultStatus, " +
            "  r.risk_level as riskLevel, " +
            "  r.content_summary as contentSummary, " +
            "  r.symptom_change as symptomChange, " +
            "  r.sbp as sbp, " +
            "  r.dbp as dbp, " +
            "  r.heart_rate as heartRate, " +
            "  r.weight_kg as weightKg, " +
            "  r.vital_measure_time as vitalMeasureTime, " +
            "  r.med_adherence as medAdherence, " +
            "  r.med_plan_summary as medPlanSummary, " +
            "  r.adverse_reaction as adverseReaction, " +
            "  r.tcm_face as tcmFace, " +
            "  r.tcm_tongue_body as tcmTongueBody, " +
            "  r.tcm_tongue_coating as tcmTongueCoating, " +
            "  r.tcm_tongue_image_url as tcmTongueImageUrl, " +
            "  r.tcm_pulse_rate as tcmPulseRate, " +
            "  r.tcm_pulse_types as tcmPulseTypes, " +
            "  r.tcm_conclusion as tcmConclusion, " +
            "  r.tcm_remark as tcmRemark, " +
            "  r.lab_summary as labSummary, " +
            "  r.next_followup_type as nextFollowupType, " +
            "  r.next_followup_date as nextFollowupDate, " +
            "  r.next_followup_remark as nextFollowupRemark, " +
            "  r.created_at as createdAt, " +
            "  r.updated_at as updatedAt, " +
            "  r.ext1 as ext1, " +
            "  r.ext2 as ext2, " +
            "  r.ext3 as ext3, " +
            "  r.ext4 as ext4, " +
            "  r.ext5 as ext5, " +
            "  s.name as staffName, " +
            "  d.name as doctorName, " +
            "  case " +
            "    when s.name is not null and trim(s.name) <> '' then s.name " +
            "    when d.name is not null and trim(d.name) <> '' then d.name " +
            "    else '医生/护士' " +
            "  end as followupStaffName " +
            "from patient_followup_record r " +
            "left join user s on s.id = r.staff_id " +
            "left join user d on d.id = r.doctor_id " +
            "where r.id = #{id} and r.patient_id = #{patientId} " +
            "limit 1")
    Map<String, Object> findPatientDetailById(@Param("patientId") Long patientId, @Param("id") Long id);

    @Select("select id, patient_id as patientId, doctor_id as doctorId, staff_id as staffId, followup_time as followupTime, followup_type as followupType, result_status as resultStatus, risk_level as riskLevel, content_summary as contentSummary, symptom_change as symptomChange, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, vital_measure_time as vitalMeasureTime, med_adherence as medAdherence, med_plan_summary as medPlanSummary, adverse_reaction as adverseReaction, tcm_face as tcmFace, tcm_tongue_body as tcmTongueBody, tcm_tongue_coating as tcmTongueCoating, tcm_tongue_image_url as tcmTongueImageUrl, tcm_pulse_rate as tcmPulseRate, tcm_pulse_types as tcmPulseTypes, tcm_conclusion as tcmConclusion, tcm_remark as tcmRemark, lab_summary as labSummary, next_followup_type as nextFollowupType, next_followup_date as nextFollowupDate, next_followup_remark as nextFollowupRemark, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_followup_record where patient_id=#{patientId} order by followup_time desc, id desc")
    List<PatientFollowupRecord> findByPatientId(@Param("patientId") Long patientId);

    @Select("select id, patient_id as patientId, doctor_id as doctorId, staff_id as staffId, followup_time as followupTime, followup_type as followupType, result_status as resultStatus, risk_level as riskLevel, content_summary as contentSummary, symptom_change as symptomChange, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, vital_measure_time as vitalMeasureTime, med_adherence as medAdherence, med_plan_summary as medPlanSummary, adverse_reaction as adverseReaction, tcm_face as tcmFace, tcm_tongue_body as tcmTongueBody, tcm_tongue_coating as tcmTongueCoating, tcm_tongue_image_url as tcmTongueImageUrl, tcm_pulse_rate as tcmPulseRate, tcm_pulse_types as tcmPulseTypes, tcm_conclusion as tcmConclusion, tcm_remark as tcmRemark, lab_summary as labSummary, next_followup_type as nextFollowupType, next_followup_date as nextFollowupDate, next_followup_remark as nextFollowupRemark, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_followup_record where patient_id=#{patientId} order by followup_time desc, id desc limit 1")
    PatientFollowupRecord findLatestByPatientId(@Param("patientId") Long patientId);

    @Select("select id, patient_id as patientId, doctor_id as doctorId, staff_id as staffId, followup_time as followupTime, followup_type as followupType, result_status as resultStatus, risk_level as riskLevel, content_summary as contentSummary, symptom_change as symptomChange, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, vital_measure_time as vitalMeasureTime, med_adherence as medAdherence, med_plan_summary as medPlanSummary, adverse_reaction as adverseReaction, tcm_face as tcmFace, tcm_tongue_body as tcmTongueBody, tcm_tongue_coating as tcmTongueCoating, tcm_tongue_image_url as tcmTongueImageUrl, tcm_pulse_rate as tcmPulseRate, tcm_pulse_types as tcmPulseTypes, tcm_conclusion as tcmConclusion, tcm_remark as tcmRemark, lab_summary as labSummary, next_followup_type as nextFollowupType, next_followup_date as nextFollowupDate, next_followup_remark as nextFollowupRemark, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_followup_record where id=#{id} and patient_id=#{patientId} limit 1")
    PatientFollowupRecord findById(@Param("patientId") Long patientId, @Param("id") Long id);

    @Select("select id, patient_id as patientId, doctor_id as doctorId, staff_id as staffId, followup_time as followupTime, followup_type as followupType, result_status as resultStatus, risk_level as riskLevel, content_summary as contentSummary, symptom_change as symptomChange, sbp, dbp, heart_rate as heartRate, weight_kg as weightKg, vital_measure_time as vitalMeasureTime, med_adherence as medAdherence, med_plan_summary as medPlanSummary, adverse_reaction as adverseReaction, tcm_face as tcmFace, tcm_tongue_body as tcmTongueBody, tcm_tongue_coating as tcmTongueCoating, tcm_tongue_image_url as tcmTongueImageUrl, tcm_pulse_rate as tcmPulseRate, tcm_pulse_types as tcmPulseTypes, tcm_conclusion as tcmConclusion, tcm_remark as tcmRemark, lab_summary as labSummary, next_followup_type as nextFollowupType, next_followup_date as nextFollowupDate, next_followup_remark as nextFollowupRemark, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_followup_record where ext4=#{taskId} order by followup_time desc, id desc limit 1")
    PatientFollowupRecord findLatestByTaskId(@Param("taskId") String taskId);

    @Select("<script>" +
            "select count(*) from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "<where> 1=1 " +
            "<if test='doctorId != null'> and r.doctor_id = #{doctorId} </if>" +
            "<if test='staffId != null'> and r.staff_id = #{staffId} </if>" +
            "<if test='startAt != null'> and r.followup_time &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and r.followup_time &lt;= #{endAt} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level) = upper(#{riskLevel}) </if>" +
            "<if test='followupType != null and followupType.trim() != \"\"'> and upper(r.followup_type) = upper(#{followupType}) </if>" +
            "<if test='resultStatus != null and resultStatus.trim() != \"\"'> and upper(r.result_status) = upper(#{resultStatus}) </if>" +
            "<if test='patientName != null and patientName.trim() != \"\"'> and p.name like concat('%', #{patientName}, '%') </if>" +
            "<if test='staffName != null and staffName.trim() != \"\"'> and s.name like concat('%', #{staffName}, '%') </if>" +
            "</where>" +
            "</script>")
    Long countFollowupRecordPage(@Param("doctorId") Long doctorId,
                                 @Param("staffId") Long staffId,
                                 @Param("startAt") java.time.LocalDateTime startAt,
                                 @Param("endAt") java.time.LocalDateTime endAt,
                                 @Param("riskLevel") String riskLevel,
                                 @Param("followupType") String followupType,
                                 @Param("resultStatus") String resultStatus,
                                 @Param("patientName") String patientName,
                                 @Param("staffName") String staffName);

    @Select("<script>" +
            "select " +
            "  r.id as id, " +
            "  r.patient_id as patientId, " +
            "  r.followup_time as followupTime, " +
            "  r.followup_type as followupType, " +
            "  r.result_status as resultStatus, " +
            "  r.risk_level as riskLevel, " +
            "  r.content_summary as contentSummary, " +
            "  r.next_followup_date as nextFollowupDate, " +
            "  p.name as patientName, " +
            "  s.name as staffName " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "<where> 1=1 " +
            "<if test='doctorId != null'> and r.doctor_id = #{doctorId} </if>" +
            "<if test='staffId != null'> and r.staff_id = #{staffId} </if>" +
            "<if test='startAt != null'> and r.followup_time &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and r.followup_time &lt;= #{endAt} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level) = upper(#{riskLevel}) </if>" +
            "<if test='followupType != null and followupType.trim() != \"\"'> and upper(r.followup_type) = upper(#{followupType}) </if>" +
            "<if test='resultStatus != null and resultStatus.trim() != \"\"'> and upper(r.result_status) = upper(#{resultStatus}) </if>" +
            "<if test='patientName != null and patientName.trim() != \"\"'> and p.name like concat('%', #{patientName}, '%') </if>" +
            "<if test='staffName != null and staffName.trim() != \"\"'> and s.name like concat('%', #{staffName}, '%') </if>" +
            "</where>" +
            "order by r.followup_time desc, r.id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<java.util.Map<String, Object>> selectFollowupRecordPage(@Param("doctorId") Long doctorId,
                                                                 @Param("staffId") Long staffId,
                                                                 @Param("startAt") java.time.LocalDateTime startAt,
                                                                 @Param("endAt") java.time.LocalDateTime endAt,
                                                                 @Param("riskLevel") String riskLevel,
                                                                 @Param("followupType") String followupType,
                                                                 @Param("resultStatus") String resultStatus,
                                                                 @Param("patientName") String patientName,
                                                                 @Param("staffName") String staffName,
                                                                 @Param("offset") Integer offset,
                                                                 @Param("limit") Integer limit);

    @Select("select " +
            "  r.*, " +
            "  p.name as patientName, " +
            "  p.phone as patientPhone, " +
            "  p.sex as patientGender, " +
            "  p.age as patientAge, " +
            "  bi.id_card as patientIdCard, " +
            "  s.name as staffName, " +
            "  d.name as doctorName " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join patient_basic_info bi on bi.patient_id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "left join user d on d.id = r.doctor_id " +
            "where r.id = #{id} limit 1")
    java.util.Map<String, Object> findDetailById(@Param("id") Long id);

    @Select("select " +
            "  r.*, " +
            "  p.name as patientName, " +
            "  p.phone as patientPhone, " +
            "  p.sex as patientGender, " +
            "  p.age as patientAge, " +
            "  bi.id_card as patientIdCard, " +
            "  s.name as staffName, " +
            "  d.name as doctorName " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join patient_basic_info bi on bi.patient_id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "left join user d on d.id = r.doctor_id " +
            "where r.id = #{id} and r.staff_id = #{staffId} limit 1")
    java.util.Map<String, Object> findDetailByIdForStaff(@Param("id") Long id, @Param("staffId") Long staffId);

    @Select("select " +
            "  r.id as id, " +
            "  date_format(r.followup_time, '%Y-%m-%d %H:%i:%s') as followupTime, " +
            "  r.followup_type as followupType, " +
            "  r.result_status as resultStatus, " +
            "  r.risk_level as riskLevel, " +
            "  r.content_summary as contentSummary, " +
            "  date_format(r.next_followup_date, '%Y-%m-%d %H:%i:%s') as nextPlanTime, " +
            "  r.ext1 as priority, " +
            "  s.name as staffName " +
            "from patient_followup_record r " +
            "left join user s on s.id = r.staff_id " +
            "where r.patient_id = #{patientId} " +
            "order by r.followup_time desc, r.id desc " +
            "limit #{limit}")
    List<java.util.Map<String, Object>> findRecentByPatientId(@Param("patientId") Long patientId,
                                                              @Param("limit") Integer limit);

    @Select("<script>" +
            "select count(distinct r.patient_id) " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "<where> 1=1 " +
            "<if test='doctorId != null'> and r.doctor_id = #{doctorId} </if>" +
            "<if test='staffId != null'> and r.staff_id = #{staffId} </if>" +
            "<if test='startAt != null'> and r.followup_time &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and r.followup_time &lt;= #{endAt} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level) = upper(#{riskLevel}) </if>" +
            "<if test='followupType != null and followupType.trim() != \"\"'> and upper(r.followup_type) = upper(#{followupType}) </if>" +
            "<if test='patientName != null and patientName.trim() != \"\"'> and p.name like concat('%', #{patientName}, '%') </if>" +
            "<if test='staffName != null and staffName.trim() != \"\"'> and s.name like concat('%', #{staffName}, '%') </if>" +
            "</where>" +
            "</script>")
    Long countDistinctPatients(@Param("doctorId") Long doctorId,
                               @Param("staffId") Long staffId,
                               @Param("startAt") java.time.LocalDateTime startAt,
                               @Param("endAt") java.time.LocalDateTime endAt,
                               @Param("riskLevel") String riskLevel,
                               @Param("followupType") String followupType,
                               @Param("patientName") String patientName,
                               @Param("staffName") String staffName);

    @Select("<script>" +
            "select upper(coalesce(r.result_status,'')) as k, count(*) as cnt " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "<where> 1=1 " +
            "<if test='doctorId != null'> and r.doctor_id = #{doctorId} </if>" +
            "<if test='staffId != null'> and r.staff_id = #{staffId} </if>" +
            "<if test='startAt != null'> and r.followup_time &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and r.followup_time &lt;= #{endAt} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level) = upper(#{riskLevel}) </if>" +
            "<if test='followupType != null and followupType.trim() != \"\"'> and upper(r.followup_type) = upper(#{followupType}) </if>" +
            "<if test='patientName != null and patientName.trim() != \"\"'> and p.name like concat('%', #{patientName}, '%') </if>" +
            "<if test='staffName != null and staffName.trim() != \"\"'> and s.name like concat('%', #{staffName}, '%') </if>" +
            "</where>" +
            "group by upper(coalesce(r.result_status,''))" +
            "</script>")
    List<java.util.Map<String, Object>> countByResultStatus(@Param("doctorId") Long doctorId,
                                                            @Param("staffId") Long staffId,
                                                            @Param("startAt") java.time.LocalDateTime startAt,
                                                            @Param("endAt") java.time.LocalDateTime endAt,
                                                            @Param("riskLevel") String riskLevel,
                                                            @Param("followupType") String followupType,
                                                            @Param("patientName") String patientName,
                                                            @Param("staffName") String staffName);

    @Select("<script>" +
            "select upper(coalesce(r.followup_type,'')) as k, count(*) as cnt " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "<where> 1=1 " +
            "<if test='doctorId != null'> and r.doctor_id = #{doctorId} </if>" +
            "<if test='staffId != null'> and r.staff_id = #{staffId} </if>" +
            "<if test='startAt != null'> and r.followup_time &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and r.followup_time &lt;= #{endAt} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level) = upper(#{riskLevel}) </if>" +
            "<if test='patientName != null and patientName.trim() != \"\"'> and p.name like concat('%', #{patientName}, '%') </if>" +
            "<if test='staffName != null and staffName.trim() != \"\"'> and s.name like concat('%', #{staffName}, '%') </if>" +
            "</where>" +
            "group by upper(coalesce(r.followup_type,''))" +
            "</script>")
    List<java.util.Map<String, Object>> countByFollowupType(@Param("doctorId") Long doctorId,
                                                            @Param("staffId") Long staffId,
                                                            @Param("startAt") java.time.LocalDateTime startAt,
                                                            @Param("endAt") java.time.LocalDateTime endAt,
                                                            @Param("riskLevel") String riskLevel,
                                                            @Param("patientName") String patientName,
                                                            @Param("staffName") String staffName);

    @Select("<script>" +
            "select upper(coalesce(r.risk_level,'')) as k, count(*) as cnt " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "left join user s on s.id = r.staff_id " +
            "<where> 1=1 " +
            "<if test='doctorId != null'> and r.doctor_id = #{doctorId} </if>" +
            "<if test='staffId != null'> and r.staff_id = #{staffId} </if>" +
            "<if test='startAt != null'> and r.followup_time &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and r.followup_time &lt;= #{endAt} </if>" +
            "<if test='followupType != null and followupType.trim() != \"\"'> and upper(r.followup_type) = upper(#{followupType}) </if>" +
            "<if test='patientName != null and patientName.trim() != \"\"'> and p.name like concat('%', #{patientName}, '%') </if>" +
            "<if test='staffName != null and staffName.trim() != \"\"'> and s.name like concat('%', #{staffName}, '%') </if>" +
            "</where>" +
            "group by upper(coalesce(r.risk_level,''))" +
            "</script>")
    List<java.util.Map<String, Object>> countByRiskLevel(@Param("doctorId") Long doctorId,
                                                         @Param("staffId") Long staffId,
                                                         @Param("startAt") java.time.LocalDateTime startAt,
                                                         @Param("endAt") java.time.LocalDateTime endAt,
                                                         @Param("followupType") String followupType,
                                                         @Param("patientName") String patientName,
                                                         @Param("staffName") String staffName);
}
