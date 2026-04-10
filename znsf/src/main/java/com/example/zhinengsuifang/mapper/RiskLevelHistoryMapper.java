package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.RiskLevelHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface RiskLevelHistoryMapper {

    @Insert("insert into risk_level_history (patient_id, risk_level, assessed_at, source, created_at) " +
            "values (#{patientId}, #{riskLevel}, #{assessedAt}, #{source}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RiskLevelHistory history);
}
