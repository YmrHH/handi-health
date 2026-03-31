package com.example.zhinengsuifang.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VisitPlanMapper {

    @Select("<script>" +
            "select count(*) " +
            "from follow_up_schedule s " +
            "join user u on u.id = s.patient_id " +
            "where u.role = 'PATIENT' " +
            "<if test='status != null and status.trim() != \"\"'> and upper(s.status) = upper(#{status}) </if>" +
            "<if test='startDate != null'> and s.due_at &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> and s.due_at &lt;= #{endDate} </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    Long countPlans(@Param("status") String status,
                    @Param("startDate") LocalDateTime startDate,
                    @Param("endDate") LocalDateTime endDate,
                    @Param("keyword") String keyword);

    @Select("<script>" +
            "select " +
            "  s.id as id, " +
            "  s.patient_id as patientId, " +
            "  u.name as patientName, " +
            "  s.due_at as visitDate, " +
            "  t.trigger_type as visitType, " +
            "  coalesce(d.name, '') as doctorName, " +
            "  s.status as status, " +
            "  coalesce(t.description, '') as remark " +
            "from follow_up_schedule s " +
            "join user u on u.id = s.patient_id " +
            "left join follow_up_task t on t.id = s.related_task_id " +
            "left join user d on d.id = t.doctor_id " +
            "where u.role = 'PATIENT' " +
            "<if test='status != null and status.trim() != \"\"'> and upper(s.status) = upper(#{status}) </if>" +
            "<if test='startDate != null'> and s.due_at &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> and s.due_at &lt;= #{endDate} </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "order by s.due_at asc, s.id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Map<String, Object>> selectPlans(@Param("status") String status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("keyword") String keyword,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);

    @Update("update follow_up_schedule set status = #{status}, updated_at = NOW() where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
