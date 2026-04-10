package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.SyndromeAssessment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface SyndromeAssessmentMapper {

    @Insert("insert into syndrome_assessment (patient_id, syndrome_code, syndrome_name, score, is_stable, assessed_at, created_by_user_id, created_at) " +
            "values (#{patientId}, #{syndromeCode}, #{syndromeName}, #{score}, #{isStable}, #{assessedAt}, #{createdByUserId}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SyndromeAssessment assessment);
}
