package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.dto.DashboardSyndromeAbnormalPatient;
import com.example.zhinengsuifang.dto.DashboardTrendPoint;
import com.example.zhinengsuifang.dto.PatientBrief;
import com.example.zhinengsuifang.dto.RiskLevelCount;
import com.example.zhinengsuifang.dto.RiskLevelTrendPoint;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DashboardMapper {

    @Select("select count(*) from follow_up_schedule where due_at >= #{startAt} and due_at < #{endAt}")
    Long countFollowUpDueInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select count(*) from follow_up_schedule where status = 'COMPLETED' and completed_at >= #{startAt} and completed_at < #{endAt}")
    Long countFollowUpCompletedInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select count(*) from health_alert where created_at >= #{startAt} and created_at < #{endAt}")
    Long countDataAlertsInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select count(*) from device_alert where created_at >= #{startAt} and created_at < #{endAt}")
    Long countDeviceAlertsInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select count(*) from user where role = 'PATIENT' and created_at >= #{startAt} and created_at < #{endAt}")
    Long countNewPatientsInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select count(*) from follow_up_task where created_at >= #{startAt} and created_at < #{endAt}")
    Long countFollowUpTasksInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select (" +
            "  (select count(*) from health_alert where created_at >= #{startAt} and created_at < #{endAt}) + " +
            "  (select count(*) from device_alert where created_at >= #{startAt} and created_at < #{endAt})" +
            ")")
    Long countAllAlertsInRange(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select count(*) from health_alert where status in ('NEW','IN_PROGRESS','FOLLOWUP_CREATED')")
    Long countProcessingHealthAlerts();

    @Select("select count(*) from device_alert where status in ('NEW','IN_PROGRESS','FOLLOWUP_CREATED')")
    Long countProcessingDeviceAlerts();

    @Select("select count(*) from health_alert where status in ('NEW','IN_PROGRESS','FOLLOWUP_CREATED') and upper(severity) in ('RED','HIGH')")
    Long countRedProcessingHealthAlerts();

    @Select("select count(*) from device_alert where status in ('NEW','IN_PROGRESS','FOLLOWUP_CREATED') and upper(severity) in ('RED','HIGH')")
    Long countRedProcessingDeviceAlerts();

    @Select("select x.alertType as alertType, x.alertId as alertId, x.patientId as patientId, x.patientName as patientName, x.patientPhone as patientPhone, " +
            "x.severity as severity, x.status as status, x.createdAt as createdAt, x.title as title, x.content as content " +
            "from (" +
            "  select 'HEALTH' as alertType, a.id as alertId, a.patient_id as patientId, u.name as patientName, u.phone as patientPhone, " +
            "         a.severity as severity, a.status as status, a.created_at as createdAt, " +
            "         '健康预警' as title, a.metric_type as content " +
            "  from health_alert a join user u on u.id = a.patient_id " +
            "  where u.role = 'PATIENT' " +
            "    and a.status in ('NEW','IN_PROGRESS','FOLLOWUP_CREATED') " +
            "    and upper(a.severity) in ('RED','HIGH') " +
            "  union all " +
            "  select 'DEVICE' as alertType, d.id as alertId, d.patient_id as patientId, u2.name as patientName, u2.phone as patientPhone, " +
            "         d.severity as severity, d.status as status, d.created_at as createdAt, " +
            "         '设备告警' as title, d.alert_message as content " +
            "  from device_alert d join user u2 on u2.id = d.patient_id " +
            "  where u2.role = 'PATIENT' " +
            "    and d.status in ('NEW','IN_PROGRESS','FOLLOWUP_CREATED') " +
            "    and upper(d.severity) in ('RED','HIGH') " +
            ") x order by x.createdAt desc limit #{limit}")
    List<Map<String, Object>> listRedProcessingAlerts(@Param("limit") Integer limit);

    @Select("select " +
            "  case " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(coalesce(l.risk_level, u.risk_level)) in ('高','高危') then 'HIGH' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('MID','MEDIUM','M','2') or trim(coalesce(l.risk_level, u.risk_level)) in ('中','中危') then 'MID' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('LOW','L','1') or trim(coalesce(l.risk_level, u.risk_level)) in ('低','低危') then 'LOW' " +
            "    else 'LOW' " +
            "  end as riskLevel, " +
            "  count(*) as count " +
            "from user u " +
            "left join (" +
            "  select patient_id, substring_index(group_concat(risk_level order by assessed_at desc), ',', 1) as risk_level " +
            "  from risk_level_history group by patient_id" +
            ") l on l.patient_id = u.id " +
            "where u.role = 'PATIENT' " +
            "group by " +
            "  case " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(coalesce(l.risk_level, u.risk_level)) in ('高','高危') then 'HIGH' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('MID','MEDIUM','M','2') or trim(coalesce(l.risk_level, u.risk_level)) in ('中','中危') then 'MID' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('LOW','L','1') or trim(coalesce(l.risk_level, u.risk_level)) in ('低','低危') then 'LOW' " +
            "    else 'LOW' " +
            "  end")
    List<RiskLevelCount> countPatientsByEffectiveRiskLevel();

    @Select("select x.day as day, x.risk_level_mapped as riskLevel, count(*) as count from (" +
            "  select date_format(assessed_at, '%Y-%m-%d') as day, patient_id, " +
            "         case " +
            "           when upper(trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1)) in ('高','高危') then 'HIGH' " +
            "           when upper(trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1))) in ('MID','MEDIUM','M','2') or trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1)) in ('中','中危') then 'MID' " +
            "           when upper(trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1))) in ('LOW','L','1') or trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1)) in ('低','低危') then 'LOW' " +
            "           else 'LOW' " +
            "         end as risk_level_mapped " +
            "  from risk_level_history " +
            "  where assessed_at >= #{startAt} and assessed_at < #{endAt} " +
            "  group by date_format(assessed_at, '%Y-%m-%d'), patient_id " +
            ") x group by x.day, x.risk_level_mapped order by x.day asc")
    List<RiskLevelTrendPoint> riskLevelTrend(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select x.month as day, x.risk_level_mapped as riskLevel, count(*) as count from (" +
            "  select date_format(assessed_at, '%Y-%m') as month, patient_id, " +
            "         case " +
            "           when upper(trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1)) in ('高','高危') then 'HIGH' " +
            "           when upper(trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1))) in ('MID','MEDIUM','M','2') or trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1)) in ('中','中危') then 'MID' " +
            "           when upper(trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1))) in ('LOW','L','1') or trim(substring_index(group_concat(risk_level order by assessed_at desc), ',', 1)) in ('低','低危') then 'LOW' " +
            "           else 'LOW' " +
            "         end as risk_level_mapped " +
            "  from risk_level_history " +
            "  where assessed_at >= #{startAt} and assessed_at < #{endAt} " +
            "  group by date_format(assessed_at, '%Y-%m'), patient_id " +
            ") x group by x.month, x.risk_level_mapped order by x.month asc")
    List<RiskLevelTrendPoint> riskLevelTrendByMonth(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select date_format(assessed_at, '%Y-%m-%d') as day, count(*) as value " +
            "from syndrome_assessment where is_stable = 1 and assessed_at >= #{startAt} and assessed_at < #{endAt} " +
            "group by date_format(assessed_at, '%Y-%m-%d') order by day asc")
    List<DashboardTrendPoint> syndromeStableTrend(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select date_format(created_at, '%Y-%m-%d') as day, count(*) as value " +
            "from health_alert where created_at >= #{startAt} and created_at < #{endAt} " +
            "group by date_format(created_at, '%Y-%m-%d') order by day asc")
    List<DashboardTrendPoint> dataAlertTrend(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select x.month as day, sum(x.value) as value from (" +
            "  select date_format(created_at, '%Y-%m') as month, count(*) as value " +
            "  from health_alert where created_at >= #{startAt} and created_at < #{endAt} " +
            "  group by date_format(created_at, '%Y-%m') " +
            "  union all " +
            "  select date_format(created_at, '%Y-%m') as month, count(*) as value " +
            "  from device_alert where created_at >= #{startAt} and created_at < #{endAt} " +
            "  group by date_format(created_at, '%Y-%m') " +
            ") x group by x.month order by x.month asc")
    List<DashboardTrendPoint> alertTrendByMonth(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("<script>" +
            "select x.day as day, sum(x.value) as value from (" +
            "  select date_format(a.created_at, '%Y-%m-%d') as day, count(*) as value, doc.address as dept " +
            "  from health_alert a " +
            "  left join patient_basic_info pbi on pbi.patient_id = a.patient_id " +
            "  left join user doc on doc.id = cast(pbi.ext1 as unsigned) " +
            "  where a.created_at >= #{startAt} and a.created_at &lt; #{endAt} " +
            "  group by date_format(a.created_at, '%Y-%m-%d'), doc.address " +
            "  union all " +
            "  select date_format(d.created_at, '%Y-%m-%d') as day, count(*) as value, doc2.address as dept " +
            "  from device_alert d " +
            "  left join patient_basic_info pbi2 on pbi2.patient_id = d.patient_id " +
            "  left join user doc2 on doc2.id = cast(pbi2.ext1 as unsigned) " +
            "  where d.created_at >= #{startAt} and d.created_at &lt; #{endAt} " +
            "  group by date_format(d.created_at, '%Y-%m-%d'), doc2.address " +
            ") x " +
            "where 1=1 " +
            "<if test=\"deptName != null and deptName.trim() != ''\">" +
            "  and x.dept is not null and x.dept like concat('%', #{deptName}, '%') " +
            "</if>" +
            "group by x.day order by x.day asc" +
            "</script>")
    List<DashboardTrendPoint> alertTrendByDayWithDept(@Param("startAt") LocalDateTime startAt,
                                                     @Param("endAt") LocalDateTime endAt,
                                                     @Param("deptName") String deptName);

    @Select("<script>" +
            "select x.month as day, sum(x.value) as value from (" +
            "  select date_format(a.created_at, '%Y-%m') as month, count(*) as value, doc.address as dept " +
            "  from health_alert a " +
            "  left join patient_basic_info pbi on pbi.patient_id = a.patient_id " +
            "  left join user doc on doc.id = cast(pbi.ext1 as unsigned) " +
            "  where a.created_at >= #{startAt} and a.created_at &lt; #{endAt} " +
            "  group by date_format(a.created_at, '%Y-%m'), doc.address " +
            "  union all " +
            "  select date_format(d.created_at, '%Y-%m') as month, count(*) as value, doc2.address as dept " +
            "  from device_alert d " +
            "  left join patient_basic_info pbi2 on pbi2.patient_id = d.patient_id " +
            "  left join user doc2 on doc2.id = cast(pbi2.ext1 as unsigned) " +
            "  where d.created_at >= #{startAt} and d.created_at &lt; #{endAt} " +
            "  group by date_format(d.created_at, '%Y-%m'), doc2.address " +
            ") x " +
            "where 1=1 " +
            "<if test=\"deptName != null and deptName.trim() != ''\">" +
            "  and x.dept is not null and x.dept like concat('%', #{deptName}, '%') " +
            "</if>" +
            "group by x.month order by x.month asc" +
            "</script>")
    List<DashboardTrendPoint> alertTrendByMonthWithDept(@Param("startAt") LocalDateTime startAt,
                                                       @Param("endAt") LocalDateTime endAt,
                                                       @Param("deptName") String deptName);

    @Select("select date_format(due_at, '%Y-%m-%d') as day, count(*) as value " +
            "from follow_up_schedule where due_at >= #{startAt} and due_at < #{endAt} " +
            "group by date_format(due_at, '%Y-%m-%d') order by day asc")
    List<DashboardTrendPoint> followUpTrend(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("select date_format(created_at, '%Y-%m') as day, count(*) as value " +
            "from follow_up_task where created_at >= #{startAt} and created_at < #{endAt} " +
            "group by date_format(created_at, '%Y-%m') order by day asc")
    List<DashboardTrendPoint> followUpTaskTrendByMonth(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Select("<script>" +
            "select date_format(t.created_at, '%Y-%m-%d') as day, count(*) as value " +
            "from follow_up_task t " +
            "left join user doc on doc.id = t.doctor_id " +
            "where t.created_at >= #{startAt} and t.created_at &lt; #{endAt} " +
            "<if test=\"deptName != null and deptName.trim() != ''\">" +
            "  and doc.address is not null and doc.address like concat('%', #{deptName}, '%') " +
            "</if>" +
            "group by date_format(t.created_at, '%Y-%m-%d') order by day asc" +
            "</script>")
    List<DashboardTrendPoint> followUpTaskTrendByDayWithDept(@Param("startAt") LocalDateTime startAt,
                                                            @Param("endAt") LocalDateTime endAt,
                                                            @Param("deptName") String deptName);

    @Select("<script>" +
            "select date_format(t.created_at, '%Y-%m') as day, count(*) as value " +
            "from follow_up_task t " +
            "left join user doc on doc.id = t.doctor_id " +
            "where t.created_at >= #{startAt} and t.created_at &lt; #{endAt} " +
            "<if test=\"deptName != null and deptName.trim() != ''\">" +
            "  and doc.address is not null and doc.address like concat('%', #{deptName}, '%') " +
            "</if>" +
            "group by date_format(t.created_at, '%Y-%m') order by day asc" +
            "</script>")
    List<DashboardTrendPoint> followUpTaskTrendByMonthWithDept(@Param("startAt") LocalDateTime startAt,
                                                              @Param("endAt") LocalDateTime endAt,
                                                              @Param("deptName") String deptName);

    @Select("<script>" +
            "select " +
            "  case " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(coalesce(l.risk_level, u.risk_level)) in ('高','高危') then 'HIGH' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('MID','MEDIUM','M','2') or trim(coalesce(l.risk_level, u.risk_level)) in ('中','中危') then 'MID' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('LOW','L','1') or trim(coalesce(l.risk_level, u.risk_level)) in ('低','低危') then 'LOW' " +
            "    else 'LOW' " +
            "  end as riskLevel, " +
            "  count(*) as count " +
            "from user u " +
            "left join patient_basic_info pbi on pbi.patient_id = u.id " +
            "left join user doc on doc.id = cast(pbi.ext1 as unsigned) " +
            "left join (" +
            "  select patient_id, substring_index(group_concat(risk_level order by assessed_at desc), ',', 1) as risk_level " +
            "  from risk_level_history group by patient_id" +
            ") l on l.patient_id = u.id " +
            "where u.role = 'PATIENT' " +
            "<if test=\"deptName != null and deptName.trim() != ''\">" +
            "  and doc.address is not null and doc.address like concat('%', #{deptName}, '%') " +
            "</if>" +
            "group by " +
            "  case " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(coalesce(l.risk_level, u.risk_level)) in ('高','高危') then 'HIGH' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('MID','MEDIUM','M','2') or trim(coalesce(l.risk_level, u.risk_level)) in ('中','中危') then 'MID' " +
            "    when upper(trim(coalesce(l.risk_level, u.risk_level))) in ('LOW','L','1') or trim(coalesce(l.risk_level, u.risk_level)) in ('低','低危') then 'LOW' " +
            "    else 'LOW' " +
            "  end" +
            "</script>")
    List<RiskLevelCount> countPatientsByEffectiveRiskLevelWithDept(@Param("deptName") String deptName);

    @Select("select " +
            "  ifnull(s.syndrome_name, '未知') as disease, " +
            "  count(distinct s.patient_id) as patientCount, " +
            "  sum(case when s.is_stable = 1 then 1 else 0 end) as stableCnt, " +
            "  sum(case when s.is_stable = 0 then 1 else 0 end) as deteriorCnt, " +
            "  count(*) as totalCnt " +
            "from syndrome_assessment s " +
            "where s.assessed_at >= #{startAt} and s.assessed_at < #{endAt} " +
            "group by ifnull(s.syndrome_name, '未知') " +
            "order by patientCount desc " +
            "limit #{limit}")
    List<Map<String, Object>> diseaseAnalysisBySyndrome(@Param("startAt") LocalDateTime startAt,
                                                        @Param("endAt") LocalDateTime endAt,
                                                        @Param("limit") Integer limit);

    @Select("select u.id as id, u.name as name, u.phone as phone, u.risk_level as riskLevel " +
            "from user u " +
            "where u.role = 'PATIENT' and upper(u.risk_level) = 'HIGH' and exists (" +
            "   select 1 from follow_up_task t where t.patient_id = u.id and t.status in ('ASSIGNED','IN_PROGRESS')" +
            ") " +
            "order by u.id desc limit #{limit}")
    List<PatientBrief> highRiskPendingList(@Param("limit") Integer limit);

    @Select("select u.id as patientId, u.name as name, u.phone as phone, u.risk_level as riskLevel, " +
            "s.syndrome_code as syndromeCode, s.syndrome_name as syndromeName, s.score as score, s.assessed_at as assessedAt " +
            "from syndrome_assessment s " +
            "join user u on u.id = s.patient_id " +
            "where u.role = 'PATIENT' and s.is_stable = 0 and s.assessed_at >= #{startAt} " +
            "order by s.assessed_at desc limit #{limit}")
    List<DashboardSyndromeAbnormalPatient> syndromeAbnormalPatients(@Param("startAt") LocalDateTime startAt,
                                                                   @Param("limit") Integer limit);
}
