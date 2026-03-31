package com.example.zhinengsuifang.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface DiseaseTypeMapper {

    @Select("select id, code, name from disease_type where status = 1 and (code like concat('%', #{q}, '%') or name like concat('%', #{q}, '%')) order by sort_no asc, id asc limit #{limit}")
    List<Map<String, Object>> search(@Param("q") String q, @Param("limit") Integer limit);

    @Select("select id, code, name, status, sort_no as sortNo from disease_type where trim(name) = trim(#{name}) limit 1")
    Map<String, Object> findByName(@Param("name") String name);

    @Select("select max(cast(substring(code, 2) as unsigned)) from disease_type where code like 'D%'")
    Long selectMaxCodeNumber();

    @Select("select max(sort_no) from disease_type")
    Integer selectMaxSortNo();

    @Insert("insert into disease_type(code, name, status, sort_no, remark, created_at, updated_at) values(#{code}, #{name}, 1, #{sortNo}, #{remark}, now(), now())")
    int insertOne(@Param("code") String code,
                  @Param("name") String name,
                  @Param("sortNo") Integer sortNo,
                  @Param("remark") String remark);

    @Select("select id, code, name, status, sort_no as sortNo from disease_type where code = #{code} limit 1")
    Map<String, Object> findByCode(@Param("code") String code);
}
