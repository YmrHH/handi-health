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
            "  ifnull(nullif(trim(pbi.ext3), ''), '未知') as disease, " +
            "  count(distinct pbi.patient_id) as patientCount, " +
            "  sum(case " +
            "        when upper(trim(coalesce(rl.risk_level, u.risk_level, 'LOW'))) in ('LOW','L','1') or trim(coalesce(rl.risk_level, u.risk_level, 'LOW')) in ('低','低危') then 1 " +
            "        else 0 end) as stableCnt, " +
            "  sum(case " +
            "        when upper(trim(coalesce(rl.risk_level, u.risk_level, 'LOW'))) in ('HIGH','H','HIGH_RISK','RED','3') or trim(coalesce(rl.risk_level, u.risk_level, 'LOW')) in ('高','高危') then 1 " +
            "        else 0 end) as deteriorCnt, " +
            "  count(distinct pbi.patient_id) as totalCnt " +
            "from patient_basic_info pbi " +
            "join user u on u.id = pbi.patient_id and u.role = 'PATIENT' " +
            "left join (" +
            "   select patient_id, substring_index(group_concat(risk_level order by assessed_at desc, id desc), ',', 1) as risk_level " +
            "   from risk_level_history group by patient_id" +
            ") rl on rl.patient_id = pbi.patient_id " +
            "where pbi.ext3 is not null and trim(pbi.ext3) <> '' " +
            "group by ifnull(nullif(trim(pbi.ext3), ''), '未知') " +
            "order by patientCount desc " +
            "limit #{limit}")
    List<Map<String, Object>> diseaseAnalysisBySyndrome(@Param("startAt") LocalDateTime startAt,
                                                        @Param("endAt") LocalDateTime endAt,
                                                        @Param("limit") Integer limit);

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
    List<Map<String, Object>> diseaseAnalysisByLegacySyndrome(@Param("startAt") LocalDateTime startAt,
                                                              @Param("endAt") LocalDateTime endAt,
                                                              @Param("limit") Integer limit);

    @Select("select t.factorName as factorName, " +
            "       count(distinct t.patientId) as patientCount, " +
            "       max(t.description) as description " +
            "from ( " +
            "   select m.patient_id as patientId, " +
            "          '晨间血压异常' as factorName, " +
            "          '近30日晨间时段血压异常患者较集中' as description " +
            "   from patient_daily_measurement m " +
            "   where m.measured_at >= #{startAt} and m.measured_at < #{endAt} " +
            "     and hour(m.measured_at) between 4 and 9 " +
            "     and ( (m.sbp is not null and m.sbp >= 140) or (m.dbp is not null and m.dbp >= 90) ) " +
            "     and ( exists (select 1 from patient_basic_info p where p.patient_id = m.patient_id and p.ext3 is not null and trim(p.ext3) = trim(#{disease})) " +
            "        or exists (select 1 from syndrome_assessment s where s.patient_id = m.patient_id and s.syndrome_name = #{disease} and s.assessed_at >= #{startAt} and s.assessed_at < #{endAt}) ) " +
            "   union all " +
            "   select m.patient_id as patientId, " +
            "          '睡眠下降' as factorName, " +
            "          '近30日睡眠时长低于6小时的患者增多' as description " +
            "   from patient_daily_measurement m " +
            "   where m.measured_at >= #{startAt} and m.measured_at < #{endAt} " +
            "     and m.sleep_hours is not null and m.sleep_hours < 6 " +
            "     and ( exists (select 1 from patient_basic_info p where p.patient_id = m.patient_id and p.ext3 is not null and trim(p.ext3) = trim(#{disease})) " +
            "        or exists (select 1 from syndrome_assessment s where s.patient_id = m.patient_id and s.syndrome_name = #{disease} and s.assessed_at >= #{startAt} and s.assessed_at < #{endAt}) ) " +
            "   union all " +
            "   select r.patient_id as patientId, " +
            "          '用药依从性不足' as factorName, " +
            "          '随访记录中存在未规律用药或依从性较差情况' as description " +
            "   from patient_followup_record r " +
            "   where r.followup_time >= #{startAt} and r.followup_time < #{endAt} " +
            "     and (r.med_adherence like '%差%' or r.med_adherence like '%不规律%' or r.med_adherence like '%漏服%' or r.med_adherence like '%一般%') " +
            "     and ( exists (select 1 from patient_basic_info p where p.patient_id = r.patient_id and p.ext3 is not null and trim(p.ext3) = trim(#{disease})) " +
            "        or exists (select 1 from syndrome_assessment s where s.patient_id = r.patient_id and s.syndrome_name = #{disease} and s.assessed_at >= #{startAt} and s.assessed_at < #{endAt}) ) " +
            "   union all " +
            "   select ts.patient_id as patientId, " +
            "          '症状评分上升' as factorName, " +
            "          '问卷结果提示近期症状信号增多或风险等级升高' as description " +
            "   from patient_tcm_survey ts " +
            "   where ts.assessed_at >= #{startAt} and ts.assessed_at < #{endAt} " +
            "     and (cast(ts.result_json as char) like '%\"level\":\"high\"%' or cast(ts.result_json as char) like '%\"level\":\"mid\"%' or cast(ts.result_json as char) like '%signals%') " +
            "     and ( exists (select 1 from patient_basic_info p where p.patient_id = ts.patient_id and p.ext3 is not null and trim(p.ext3) = trim(#{disease})) " +
            "        or exists (select 1 from syndrome_assessment s where s.patient_id = ts.patient_id and s.syndrome_name = #{disease} and s.assessed_at >= #{startAt} and s.assessed_at < #{endAt}) ) " +
            "   union all " +
            "   select m.patient_id as patientId, " +
            "          '血糖波动' as factorName, " +
            "          '近30日血糖偏高或偏低波动明显' as description " +
            "   from patient_daily_measurement m " +
            "   where m.measured_at >= #{startAt} and m.measured_at < #{endAt} " +
            "     and m.glucose is not null " +
            "     and (m.glucose >= 7.0 or m.glucose <= 3.9) " +
            "     and ( exists (select 1 from patient_basic_info p where p.patient_id = m.patient_id and p.ext3 is not null and trim(p.ext3) = trim(#{disease})) " +
            "        or exists (select 1 from syndrome_assessment s where s.patient_id = m.patient_id and s.syndrome_name = #{disease} and s.assessed_at >= #{startAt} and s.assessed_at < #{endAt}) ) " +
            ") t " +
            "group by t.factorName " +
            "order by patientCount desc " +
            "limit #{limit}")
    List<Map<String, Object>> diseaseDeteriorationFactorsRaw(@Param("startAt") LocalDateTime startAt,
                                                             @Param("endAt") LocalDateTime endAt,
                                                             @Param("disease") String disease,
                                                             @Param("limit") Integer limit);

    @Select("select u.id as patientId, " +
            "       ifnull(u.age, 0) as age, " +
            "       coalesce( " +
            "           nullif(case when p.ext5 is not null and json_valid(p.ext5) then json_unquote(json_extract(p.ext5, '$.constitution')) else '' end, ''), " +
            "           '未标注体质' " +
            "       ) as constitution, " +
            "       coalesce( " +
            "           nullif(case when p.ext5 is not null and json_valid(p.ext5) then json_unquote(json_extract(p.ext5, '$.pastHistory')) else '' end, ''), " +
            "           '' " +
            "       ) as pastHistory, " +
            "       coalesce(rl.risk_level, u.risk_level, 'LOW') as riskLevel, " +
            "       m.sbp as sbp, " +
            "       m.dbp as dbp, " +
            "       m.glucose as glucose, " +
            "       m.sleep_hours as sleep, " +
            "       m.symptoms as symptoms, " +
            "       coalesce(r.med_adherence, '') as medAdherence, " +
            "       coalesce(r.tcm_conclusion, '') as tcmConclusion " +
            "from ( " +
            "   select distinct p.patient_id as patient_id from patient_basic_info p where p.ext3 is not null and trim(p.ext3) = trim(#{disease}) " +
            "   union " +
            "   select distinct s.patient_id as patient_id from syndrome_assessment s where s.syndrome_name = #{disease} and s.assessed_at >= #{startAt} and s.assessed_at < #{endAt} " +
            ") dc " +
            "join user u on u.id = dc.patient_id and u.role = 'PATIENT' " +
            "left join patient_basic_info p on p.patient_id = u.id " +
            "left join risk_level_history rl on rl.id = ( " +
            "   select rl2.id from risk_level_history rl2 where rl2.patient_id = u.id order by rl2.assessed_at desc, rl2.id desc limit 1 " +
            ") " +
            "left join patient_daily_measurement m on m.id = ( " +
            "   select m2.id from patient_daily_measurement m2 where m2.patient_id = u.id and m2.measured_at >= #{startAt} and m2.measured_at < #{endAt} order by m2.measured_at desc, m2.id desc limit 1 " +
            ") " +
            "left join patient_followup_record r on r.id = ( " +
            "   select r2.id from patient_followup_record r2 where r2.patient_id = u.id and r2.followup_time >= #{startAt} and r2.followup_time < #{endAt} order by r2.followup_time desc, r2.id desc limit 1 " +
            ")")
    List<Map<String, Object>> diseasePatientInsightRows(@Param("startAt") LocalDateTime startAt,
                                                        @Param("endAt") LocalDateTime endAt,
                                                        @Param("disease") String disease);

    @Select("select x.planName as planName, " +
            "       x.planType as planType, " +
            "       count(distinct x.patientId) as patientCount, " +
            "       sum(x.successFlag) as successCnt, " +
            "       round(avg(x.improvementDays), 1) as avgImprovementDays " +
            "from ( " +
            "   select ir.patient_id as patientId, " +
            "          case " +
            "             when ir.recommendation like '%饮食%' then '饮食调养' " +
            "             when ir.recommendation like '%运动%' or ir.recommendation like '%康复%' then '运动康复' " +
            "             when ir.recommendation like '%用药%' or ir.recommendation like '%服药%' then '用药管理' " +
            "             when ir.recommendation like '%中医%' or ir.recommendation like '%艾灸%' or ir.recommendation like '%泡脚%' then '中医调理' " +
            "             else '综合康养' " +
            "          end as planName, " +
            "          case " +
            "             when ir.recommendation like '%饮食%' then '饮食调养' " +
            "             when ir.recommendation like '%运动%' or ir.recommendation like '%康复%' then '运动康复' " +
            "             when ir.recommendation like '%用药%' or ir.recommendation like '%服药%' then '用药管理' " +
            "             when ir.recommendation like '%中医%' or ir.recommendation like '%艾灸%' or ir.recommendation like '%泡脚%' then '中医调理' " +
            "             else '综合康养' " +
            "          end as planType, " +
            "          case when exists ( " +
            "               select 1 from syndrome_assessment s2 " +
            "               where s2.patient_id = ir.patient_id " +
            "                 and s2.assessed_at >= ifnull(ir.sent_time, ir.created_at) " +
            "                 and s2.assessed_at < date_add(ifnull(ir.sent_time, ir.created_at), interval 14 day) " +
            "                 and s2.is_stable = 1 " +
            "          ) or exists ( " +
            "               select 1 from risk_level_history rh2 " +
            "               where rh2.patient_id = ir.patient_id " +
            "                 and rh2.assessed_at >= ifnull(ir.sent_time, ir.created_at) " +
            "                 and rh2.assessed_at < date_add(ifnull(ir.sent_time, ir.created_at), interval 14 day) " +
            "                 and (upper(trim(rh2.risk_level)) in ('LOW','L','1') or trim(rh2.risk_level) in ('低','低危')) " +
            "          ) then 1 else 0 end as successFlag, " +
            "          ifnull(( " +
            "               select least(14, timestampdiff(day, ifnull(ir.sent_time, ir.created_at), min(x.assessed_at))) from ( " +
            "                   select s2.assessed_at as assessed_at from syndrome_assessment s2 " +
            "                   where s2.patient_id = ir.patient_id and s2.assessed_at >= ifnull(ir.sent_time, ir.created_at) and s2.assessed_at < date_add(ifnull(ir.sent_time, ir.created_at), interval 14 day) and s2.is_stable = 1 " +
            "                   union all " +
            "                   select rh2.assessed_at as assessed_at from risk_level_history rh2 " +
            "                   where rh2.patient_id = ir.patient_id and rh2.assessed_at >= ifnull(ir.sent_time, ir.created_at) and rh2.assessed_at < date_add(ifnull(ir.sent_time, ir.created_at), interval 14 day) and (upper(trim(rh2.risk_level)) in ('LOW','L','1') or trim(rh2.risk_level) in ('低','低危')) " +
            "               ) x " +
            "          ), 14) as improvementDays " +
            "   from intervention_recommend ir " +
            "   left join patient_basic_info pbi on pbi.patient_id = ir.patient_id " +
            "   where (trim(coalesce(nullif(ir.disease, ''), pbi.ext3, #{disease})) = trim(#{disease}) " +
            "          or exists (select 1 from syndrome_assessment s0 where s0.patient_id = ir.patient_id and s0.syndrome_name = #{disease})) " +
            "     and upper(ifnull(ir.status, 'PENDING')) in ('SENT','PENDING') " +
            "     and ifnull(ir.sent_time, ir.created_at) >= #{startAt} " +
            "     and ifnull(ir.sent_time, ir.created_at) < #{endAt} " +
            ") x " +
            "group by x.planName, x.planType " +
            "order by patientCount desc " +
            "limit #{limit}")
    List<Map<String, Object>> diseaseCarePlanEffectivenessRaw(@Param("startAt") LocalDateTime startAt,
                                                              @Param("endAt") LocalDateTime endAt,
                                                              @Param("disease") String disease,
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
