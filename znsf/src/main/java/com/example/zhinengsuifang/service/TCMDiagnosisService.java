package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.entity.TCMDiagnosis;

import java.util.List;
import java.util.Map;

public interface TCMDiagnosisService {
    Map<String, Object> createTCMDiagnosis(TCMDiagnosis diagnosis);
    Map<String, Object> updateTCMDiagnosis(TCMDiagnosis diagnosis);
    Map<String, Object> getTCMDiagnosisById(Long id);
    Map<String, Object> getLatestTCMDiagnosisByPatientId(Long patientId);
    Map<String, Object> getTCMDiagnosisListByPatientId(Long patientId, int limit);
    Map<String, Object> deleteTCMDiagnosisById(Long id);
}