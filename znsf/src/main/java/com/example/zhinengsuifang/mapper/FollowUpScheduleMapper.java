package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.FollowUpSchedule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface FollowUpScheduleMapper {

    @Insert("INSERT INTO follow_up_schedule (patient_id, plan_id, due_at, status, completed_at, related_task_id, created_at, updated_at) " +
            "VALUES (#{patientId}, #{planId}, #{dueAt}, #{status}, #{completedAt}, #{relatedTaskId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(FollowUpSchedule schedule);

    @Update("update follow_up_schedule set status = #{status}, completed_at = #{completedAt}, updated_at = NOW() where related_task_id = #{taskId}")
    int updateByTaskId(@Param("taskId") Long taskId,
                       @Param("status") String status,
                       @Param("completedAt") LocalDateTime completedAt);

    @Update("update follow_up_schedule set due_at = #{dueAt}, status = #{status}, completed_at = null, updated_at = NOW() where related_task_id = #{taskId}")
    int rescheduleByTaskId(@Param("taskId") Long taskId,
                           @Param("dueAt") LocalDateTime dueAt,
                           @Param("status") String status);
}
