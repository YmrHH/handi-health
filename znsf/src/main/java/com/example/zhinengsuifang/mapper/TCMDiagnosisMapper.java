package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.TCMDiagnosis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCMDiagnosisMapper {
    int insert(TCMDiagnosis diagnosis);
    int update(TCMDiagnosis diagnosis);
    TCMDiagnosis selectById(Long id);
    List<TCMDiagnosis> selectByPatientId(@Param("patientId") Long patientId, @Param("limit") int limit);
    TCMDiagnosis selectLatestByPatientId(Long patientId);
    int deleteById(Long id);
}