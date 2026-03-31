package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.DoctorAdvice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface DoctorAdviceMapper {

    @Insert("INSERT INTO doctor_advice (advice_date, doctor_id, doctor_name, title, description, patients_json, created_at, updated_at) " +
            "VALUES (#{adviceDate}, #{doctorId}, #{doctorName}, #{title}, #{description}, #{patientsJson}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DoctorAdvice advice);

    @Select("<script>" +
            "select id, advice_date as adviceDate, doctor_id as doctorId, doctor_name as doctorName, title, description, patients_json as patientsJson, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 " +
            "from doctor_advice where 1=1 " +
            "<if test='doctorId != null'> and doctor_id = #{doctorId} </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'> and (title like concat('%', #{keyword}, '%') or description like concat('%', #{keyword}, '%')) </if>" +
            "<if test='startAt != null'> and advice_date &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and advice_date &lt;= #{endAt} </if>" +
            "order by advice_date desc, id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<DoctorAdvice> selectPage(@Param("doctorId") Long doctorId,
                                 @Param("keyword") String keyword,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    @Select("<script>" +
            "select count(*) from doctor_advice where 1=1 " +
            "<if test='doctorId != null'> and doctor_id = #{doctorId} </if>" +
            "<if test='keyword != null and keyword.trim() != \"\"'> and (title like concat('%', #{keyword}, '%') or description like concat('%', #{keyword}, '%')) </if>" +
            "<if test='startAt != null'> and advice_date &gt;= #{startAt} </if>" +
            "<if test='endAt != null'> and advice_date &lt;= #{endAt} </if>" +
            "</script>")
    Long count(@Param("doctorId") Long doctorId,
               @Param("keyword") String keyword,
               @Param("startAt") LocalDateTime startAt,
               @Param("endAt") LocalDateTime endAt);

    @Select("select id, advice_date as adviceDate, doctor_id as doctorId, doctor_name as doctorName, title, description, patients_json as patientsJson, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from doctor_advice order by advice_date desc, id desc limit #{limit}")
    List<DoctorAdvice> selectLatest(@Param("limit") int limit);

    @Select("select id, advice_date as adviceDate, doctor_id as doctorId, doctor_name as doctorName, title, description, patients_json as patientsJson, created_at as createdAt, updated_at as updatedAt, ext1, ext2, ext3, ext4, ext5 from doctor_advice where id = #{id} limit 1")
    DoctorAdvice findById(@Param("id") Long id);
}
