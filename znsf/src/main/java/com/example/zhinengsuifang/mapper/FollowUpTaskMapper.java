package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.FollowUpTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 随访任务（派单）表相关 Mapper。
 */
public interface FollowUpTaskMapper {

    @Insert("INSERT INTO follow_up_task (patient_id, doctor_id, follow_up_user_id, trigger_type, description, status, created_at, updated_at, ext1, ext2) " +
            "VALUES (#{patientId}, #{doctorId}, #{followUpUserId}, #{triggerType}, #{description}, #{status}, NOW(), NOW(), #{ext1}, #{ext2})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /**
     * 新增随访任务。
     */
    void insert(FollowUpTask task);

    @Select("select * from follow_up_task where doctor_id = #{doctorId} order by created_at desc")
    /**
     * 查询医生派出的任务列表。
     */
    List<FollowUpTask> findByDoctorId(@Param("doctorId") Long doctorId);

    @Select("select * from follow_up_task where follow_up_user_id = #{followUpUserId} order by created_at desc")
    /**
     * 查询随访员名下的任务列表。
     */
    List<FollowUpTask> findByFollowUpUserId(@Param("followUpUserId") Long followUpUserId);

    @Select("select * from follow_up_task where id = #{id}")
    /**
     * 按 id 查询任务。
     */
    FollowUpTask findById(@Param("id") Long id);

    @Update("update follow_up_task set status = #{status}, updated_at = NOW() where id = #{id}")
    /**
     * 更新任务状态。
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("select count(*) from follow_up_task where doctor_id = #{doctorId} and patient_id = #{patientId}")
    /**
     * 统计医生對某患者的派单数量，用於权限校驗。
     */
    Long countByDoctorIdAndPatientId(@Param("doctorId") Long doctorId, @Param("patientId") Long patientId);

    @Select("select * from follow_up_task where patient_id = #{patientId} and status = #{status} order by created_at desc limit 1")
    /**
     * 按患者ID和状态查询最新一条任务。
     */
    FollowUpTask findByPatientIdAndStatus(@Param("patientId") Long patientId, @Param("status") String status);

    @Select("select * from follow_up_task where patient_id = #{patientId} and upper(status) in ('ASSIGNED','PENDING','IN_PROGRESS') order by created_at desc limit 1")
    FollowUpTask findLatestActiveByPatientId(@Param("patientId") Long patientId);

    @Update("update follow_up_task set trigger_type = #{triggerType}, description = #{description}, " +
            "follow_up_user_id = #{followUpUserId}, ext1 = #{ext1}, ext2 = #{ext2}, updated_at = NOW() where id = #{id}")
    /**
     * 更新任务详细信息。
     */
    int updateTaskInfo(@Param("id") Long id, @Param("triggerType") String triggerType,
                       @Param("description") String description, @Param("followUpUserId") Long followUpUserId,
                       @Param("ext1") String ext1, @Param("ext2") String ext2);

    @Select("select " +
            "  t.id as taskId, " +
            "  p.name as patientName, " +
            "  p.address as address, " +
            "  t.trigger_type as serviceType, " +
            "  t.ext1 as planDate, " +
            "  fu.name as assignee, " +
            "  t.follow_up_user_id as staffId, " +
            "  fu.name as staffName, " +
            "  t.status as status, " +
            "  t.description as remark " +
            "from follow_up_task t " +
            "join user p on p.id = t.patient_id " +
            "join user fu on fu.id = t.follow_up_user_id " +
            "where t.doctor_id = #{doctorId} " +
            "order by t.created_at desc")
    List<java.util.Map<String, Object>> selectHomeServiceTasksByDoctor(@Param("doctorId") Long doctorId);

    @Update("update follow_up_task set follow_up_user_id = #{followUpUserId}, updated_at = NOW() where id = #{id}")
    int updateFollowUpUser(@Param("id") Long id, @Param("followUpUserId") Long followUpUserId);

    @Select("select count(*) " +
            "from follow_up_task t " +
            "left join follow_up_schedule s on s.related_task_id = t.id " +
            "where t.patient_id = #{patientId} " +
            "and upper(t.status) in ('ASSIGNED','PENDING')")
    Long countPendingByPatient(@Param("patientId") Long patientId);

    @Select("select count(*) from follow_up_task t " +
            "where t.follow_up_user_id = #{followUpUserId} " +
            "and upper(coalesce(t.status,'')) in ('ASSIGNED','PENDING','IN_PROGRESS')")
    Long countActiveByFollowUpUserId(@Param("followUpUserId") Long followUpUserId);

    @Select("select count(*) " +
            "from follow_up_task t " +
            "left join follow_up_schedule s on s.related_task_id = t.id " +
            "where t.patient_id = #{patientId} " +
            "and upper(t.status) in ('ASSIGNED','PENDING') " +
            "and s.due_at is not null and s.due_at < now() " +
            "and (s.status is null or upper(s.status) <> 'COMPLETED')")
    Long countOverdueByPatient(@Param("patientId") Long patientId);

    @Select("select " +
            "  t.id as id, " +
            "  date_format(s.due_at, '%Y-%m-%d') as visitDate, " +
            "  date_format(s.due_at, '%H:%i') as visitTime, " +
            "  t.description as symptom, " +
            "  '' as systemSyndrome, " +
            "  d.name as doctor, " +
            "  t.ext2 as priority, " +
            "  case when s.due_at is not null and s.due_at < now() then 1 else 0 end as overdue " +
            "from follow_up_task t " +
            "left join follow_up_schedule s on s.related_task_id = t.id " +
            "left join user d on d.id = t.doctor_id " +
            "where t.patient_id = #{patientId} " +
            "and upper(t.status) in ('ASSIGNED','PENDING') " +
            "order by s.due_at asc, t.id desc " +
            "limit #{limit}")
    List<java.util.Map<String, Object>> selectPendingTasksByPatient(@Param("patientId") Long patientId,
                                                                    @Param("limit") Integer limit);
}

