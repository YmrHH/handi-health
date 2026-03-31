package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.InterventionRecommend;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InterventionRecommendMapper {

    @Select("select id, source_type as sourceType, source_id as sourceId, patient_id as patientId, patient_name as patientName, risk_level as riskLevel, trigger_reason as triggerReason, trigger_time as triggerTime, body_type as bodyType, pattern, disease, doctor, status, recommendation, sent_time as sentTime, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from intervention_recommend where id=#{id} limit 1")
    InterventionRecommend findById(@Param("id") Long id);

    @Select("select id, source_type as sourceType, source_id as sourceId, patient_id as patientId, patient_name as patientName, risk_level as riskLevel, trigger_reason as triggerReason, trigger_time as triggerTime, body_type as bodyType, pattern, disease, doctor, status, recommendation, sent_time as sentTime, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from intervention_recommend where upper(source_type)=upper(#{sourceType}) and source_id=#{sourceId} limit 1")
    InterventionRecommend findBySource(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    @Insert("insert into intervention_recommend (source_type, source_id, patient_id, patient_name, risk_level, trigger_reason, trigger_time, body_type, pattern, disease, doctor, status, recommendation, sent_time, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) " +
            "values (#{sourceType}, #{sourceId}, #{patientId}, #{patientName}, #{riskLevel}, #{triggerReason}, #{triggerTime}, #{bodyType}, #{pattern}, #{disease}, #{doctor}, #{status}, #{recommendation}, #{sentTime}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterventionRecommend record);

    @Update("update intervention_recommend set patient_id=#{patientId}, patient_name=#{patientName}, risk_level=#{riskLevel}, trigger_reason=#{triggerReason}, trigger_time=#{triggerTime}, body_type=#{bodyType}, pattern=#{pattern}, disease=#{disease}, doctor=#{doctor}, updated_at=now() where id=#{id}")
    int updateMeta(InterventionRecommend record);

    @Update("update intervention_recommend set recommendation=#{recommendation}, updated_at=now() where id=#{id}")
    int updateDraft(@Param("id") Long id, @Param("recommendation") String recommendation);

    @Update("update intervention_recommend set status='SENT', recommendation=#{recommendation}, sent_time=now(), updated_at=now() where id=#{id}")
    int markSent(@Param("id") Long id, @Param("recommendation") String recommendation);

    @Select("<script>" +
            "select count(*) from intervention_recommend r where 1=1 " +
            "<if test='sourceType != null and sourceType.trim() != \"\"'> and upper(r.source_type)=upper(#{sourceType}) </if>" +
            "<if test='status != null and status.trim() != \"\"'> and upper(r.status)=upper(#{status}) </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level)=upper(#{riskLevel}) </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            " and (r.patient_name like concat('%', #{keyword}, '%') or cast(r.patient_id as char) like concat('%', #{keyword}, '%'))" +
            "</if>" +
            "</script>")
    Long countList(@Param("sourceType") String sourceType,
                   @Param("status") String status,
                   @Param("riskLevel") String riskLevel,
                   @Param("keyword") String keyword);

    @Select("<script>" +
            "select " +
            "  r.id as id, " +
            "  r.source_type as sourceType, " +
            "  r.source_id as sourceId, " +
            "  r.patient_id as patientId, " +
            "  r.patient_name as patientName, " +
            "  r.risk_level as riskLevel, " +
            "  r.trigger_reason as triggerReason, " +
            "  date_format(r.trigger_time, '%Y-%m-%d %H:%i:%s') as triggerTime, " +
            "  r.status as status, " +
            "  date_format(r.sent_time, '%Y-%m-%d %H:%i:%s') as sentTime, " +
            "  r.body_type as bodyType, " +
            "  r.pattern as pattern, " +
            "  r.disease as disease, " +
            "  r.doctor as doctor, " +
            "  r.recommendation as recommendation " +
            "from intervention_recommend r where 1=1 " +
            "<if test='sourceType != null and sourceType.trim() != \"\"'> and upper(r.source_type)=upper(#{sourceType}) </if>" +
            "<if test='status != null and status.trim() != \"\"'> and upper(r.status)=upper(#{status}) </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(r.risk_level)=upper(#{riskLevel}) </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            " and (r.patient_name like concat('%', #{keyword}, '%') or cast(r.patient_id as char) like concat('%', #{keyword}, '%'))" +
            "</if>" +
            "order by r.created_at desc, r.id desc" +
            "</script>")
    List<Map<String, Object>> selectList(@Param("sourceType") String sourceType,
                                         @Param("status") String status,
                                         @Param("riskLevel") String riskLevel,
                                         @Param("keyword") String keyword);

    @Select("<script>" +
            "select count(*) from intervention_recommend r where 1=1 " +
            "and upper(r.status)='SENT' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            " and (r.patient_name like concat('%', #{keyword}, '%') " +
            "      or cast(r.patient_id as char) like concat('%', #{keyword}, '%') " +
            "      or r.recommendation like concat('%', #{keyword}, '%') " +
            "      or r.doctor like concat('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='startAt != null'> and ifnull(r.sent_time, r.created_at) &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and ifnull(r.sent_time, r.created_at) &lt;= #{endAt} </if>" +
            "</script>")
    Long countSent(@Param("keyword") String keyword,
                   @Param("startAt") LocalDateTime startAt,
                   @Param("endAt") LocalDateTime endAt);

    @Select("<script>" +
            "select " +
            "  r.id as id, " +
            "  r.patient_id as patientId, " +
            "  r.patient_name as patientName, " +
            "  r.doctor as doctor, " +
            "  r.recommendation as recommendation, " +
            "  date_format(ifnull(r.sent_time, r.created_at), '%Y-%m-%d %H:%i:%s') as adviceDate " +
            "from intervention_recommend r where 1=1 " +
            "and upper(r.status)='SENT' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            " and (r.patient_name like concat('%', #{keyword}, '%') " +
            "      or cast(r.patient_id as char) like concat('%', #{keyword}, '%') " +
            "      or r.recommendation like concat('%', #{keyword}, '%') " +
            "      or r.doctor like concat('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='startAt != null'> and ifnull(r.sent_time, r.created_at) &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and ifnull(r.sent_time, r.created_at) &lt;= #{endAt} </if>" +
            "order by ifnull(r.sent_time, r.created_at) desc, r.id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Map<String, Object>> selectSentPage(@Param("keyword") String keyword,
                                             @Param("startAt") LocalDateTime startAt,
                                             @Param("endAt") LocalDateTime endAt,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);
}
