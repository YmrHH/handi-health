package com.example.zhinengsuifang.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FollowUpWorkbenchMapper {

    @Select("<script>" +
            "select count(*) " +
            "from follow_up_task t " +
            "join user p on p.id = t.patient_id " +
            "left join user fu on fu.id = t.follow_up_user_id " +
            "left join follow_up_schedule s on s.related_task_id = t.id " +
            "where upper(trim(p.role)) = 'PATIENT' " +
            "<if test='status != null and status.trim() != \"\"'> and upper(trim(t.status)) = upper(trim(#{status})) </if>" +
            "<if test='staffId != null'> and t.follow_up_user_id = #{staffId} </if>" +
            "<if test='doctorId != null'> and t.doctor_id = #{doctorId} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(trim(p.risk_level)) = upper(trim(#{riskLevel})) </if>" +
            "<if test='startAt != null'> and s.due_at &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and s.due_at &lt;= #{endAt} </if>" +
            "</script>")
    Long countTasks(@Param("status") String status,
                    @Param("staffId") Long staffId,
                    @Param("doctorId") Long doctorId,
                    @Param("riskLevel") String riskLevel,
                    @Param("startAt") LocalDateTime startAt,
                    @Param("endAt") LocalDateTime endAt);

    @Select("<script>" +
            "select " +
            "  t.id as taskId, " +
            "  t.patient_id as patientId, " +
            "  t.patient_id as patientIdNumber, " +
            "  p.name as patientName, " +
            "  p.phone as phone, " +
            "  p.risk_level as riskLevel, " +
            "  pbi.id_card as idCard, " +
            "  pbi.ext3 as disease, " +
            "  coalesce(d.name, d.username, '') as doctor, " +
            "  null as mainDisease, " +
            "  s.due_at as planTime, " +
            "  s.completed_at as finishTime, " +
            "  t.trigger_type as followMethod, " +
            "  t.status as status, " +
            "  t.created_at as createdAt, " +
            "  t.follow_up_user_id as taskStaffId, " +
            "  t.ext1 as planDate, " +
            "  t.ext2 as checklist, " +
            "  t.description as followupContent, " +
            "  fu.name as staffName, " +
            "  fu.id as staffId " +
            "from follow_up_task t " +
            "join user p on p.id = t.patient_id " +
            "left join patient_basic_info pbi on pbi.patient_id = t.patient_id " +
            "left join user d on d.id = t.doctor_id " +
            "left join user fu on fu.id = t.follow_up_user_id " +
            "left join follow_up_schedule s on s.related_task_id = t.id " +
            "where upper(trim(p.role)) = 'PATIENT' " +
            "<if test='status != null and status.trim() != \"\"'> and upper(trim(t.status)) = upper(trim(#{status})) </if>" +
            "<if test='staffId != null'> and t.follow_up_user_id = #{staffId} </if>" +
            "<if test='doctorId != null'> and t.doctor_id = #{doctorId} </if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'> and upper(trim(p.risk_level)) = upper(trim(#{riskLevel})) </if>" +
            "<if test='startAt != null'> and s.due_at &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and s.due_at &lt;= #{endAt} </if>" +
            "order by s.due_at asc, t.id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Map<String, Object>> selectTasks(@Param("status") String status,
                                         @Param("staffId") Long staffId,
                                         @Param("doctorId") Long doctorId,
                                         @Param("riskLevel") String riskLevel,
                                         @Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);
}
