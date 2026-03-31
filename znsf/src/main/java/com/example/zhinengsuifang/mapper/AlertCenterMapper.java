package com.example.zhinengsuifang.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AlertCenterMapper {

    @Select("<script>" +
            "select count(*) from (" +
            "  select x.source, x.id, x.patientId, x.level, x.status, x.alertTime " +
            "  from (" +
            "    select 'HEALTH' as source, a.id as id, a.patient_id as patientId, a.severity as level, a.status as status, a.created_at as alertTime " +
            "    from health_alert a " +
            "    union all " +
            "    select 'DEVICE' as source, d.id as id, d.patient_id as patientId, d.severity as level, d.status as status, d.created_at as alertTime " +
            "    from device_alert d " +
            "  ) x " +
            "  join user u on u.id = x.patientId " +
            "  where u.role = 'PATIENT' " +
            "<if test='patientId != null'> and x.patientId = #{patientId} </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='level != null and level.trim() != \"\"'>" +
            "  and upper(x.level) = upper(#{level}) " +
            "</if>" +
            "<if test='status != null and status.trim() != \"\"'>" +
            "  and upper(x.status) = upper(#{status}) " +
            "</if>" +
            "<if test='source != null and source.trim() != \"\"'>" +
            "  and upper(x.source) = upper(#{source}) " +
            "</if>" +
            "<if test='startAt != null'> and x.alertTime &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and x.alertTime &lt;= #{endAt} </if>" +
            " ) t" +
            "</script>")
    Long countAlertCenter(@Param("patientId") Long patientId,
                          @Param("keyword") String keyword,
                          @Param("level") String level,
                          @Param("status") String status,
                          @Param("source") String source,
                          @Param("startAt") LocalDateTime startAt,
                          @Param("endAt") LocalDateTime endAt);

    @Select("<script>" +
            "select " +
            "  concat(x.source, '-', x.id) as id, " +
            "  x.source as source, " +
            "  x.id as alertId, " +
            "  x.patientId as patientId, " +
            "  u.name as patientName, " +
            "  null as mainDisease, " +
            "  u.risk_level as riskLevel, " +
            "  x.level as level, " +
            "  x.alertTime as alertTime, " +
            "  x.status as status, " +
            "  '' as doctorName, " +
            "  '' as handlerName, " +
            "  x.summary as summary " +
            "from (" +
            "  select 'HEALTH' as source, a.id as id, a.patient_id as patientId, a.severity as level, a.status as status, a.created_at as alertTime, " +
            "         concat(ifnull(a.metric_type,''), ' ', ifnull(a.delta_value1,'')) as summary " +
            "  from health_alert a " +
            "  union all " +
            "  select 'DEVICE' as source, d.id as id, d.patient_id as patientId, d.severity as level, d.status as status, d.created_at as alertTime, " +
            "         concat(ifnull(d.device_sn,''), ' ', ifnull(d.alert_message,'')) as summary " +
            "  from device_alert d " +
            ") x " +
            "join user u on u.id = x.patientId " +
            "where u.role = 'PATIENT' " +
            "<if test='patientId != null'> and x.patientId = #{patientId} </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='level != null and level.trim() != \"\"'>" +
            "  and upper(x.level) = upper(#{level}) " +
            "</if>" +
            "<if test='status != null and status.trim() != \"\"'>" +
            "  and upper(x.status) = upper(#{status}) " +
            "</if>" +
            "<if test='source != null and source.trim() != \"\"'>" +
            "  and upper(x.source) = upper(#{source}) " +
            "</if>" +
            "<if test='startAt != null'> and x.alertTime &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and x.alertTime &lt;= #{endAt} </if>" +
            "order by x.alertTime desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Map<String, Object>> selectAlertCenterPage(@Param("patientId") Long patientId,
                                                    @Param("keyword") String keyword,
                                                    @Param("level") String level,
                                                    @Param("status") String status,
                                                    @Param("source") String source,
                                                    @Param("startAt") LocalDateTime startAt,
                                                    @Param("endAt") LocalDateTime endAt,
                                                    @Param("offset") Integer offset,
                                                    @Param("limit") Integer limit);

    @Select("select t.level from (" +
            "  select a.severity as level from health_alert a where a.id = #{alertId} " +
            "  union all " +
            "  select d.severity as level from device_alert d where d.id = #{alertId} " +
            ") t limit 1")
    String findAlertSeverity(@Param("alertId") Long alertId);
}
