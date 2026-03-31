package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.PatientRehabTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PatientRehabTaskMapper {

    @Select("select id, patient_id as patientId, title, description, difficulty, status, start_date as startDate, due_date as dueDate, completed_at as completedAt, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from patient_rehab_task where patient_id=#{patientId} order by due_date asc, id desc")
    List<PatientRehabTask> findByPatientId(@Param("patientId") Long patientId);

    @Insert("insert into patient_rehab_task (patient_id, title, description, difficulty, status, start_date, due_date, completed_at, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{patientId}, #{title}, #{description}, #{difficulty}, #{status}, #{startDate}, #{dueDate}, #{completedAt}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientRehabTask t);

    @Update("update patient_rehab_task set status=#{status}, completed_at=case when upper(#{status}) in ('DONE','COMPLETED') then now() else completed_at end, updated_at=now() where id=#{id} and patient_id=#{patientId}")
    int updateStatus(@Param("patientId") Long patientId, @Param("id") Long id, @Param("status") String status);

    @Update("update patient_rehab_task set difficulty=#{difficulty}, updated_at=now() where id=#{id} and patient_id=#{patientId}")
    int updateDifficulty(@Param("patientId") Long patientId, @Param("id") Long id, @Param("difficulty") String difficulty);
}
