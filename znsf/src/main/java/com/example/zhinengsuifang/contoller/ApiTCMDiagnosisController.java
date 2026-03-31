package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.entity.TCMDiagnosis;
import com.example.zhinengsuifang.service.TCMDiagnosisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tcm-diagnosis")
@Tag(name = "中医四诊", description = "中医四诊与证候相关接口")
public class ApiTCMDiagnosisController {

    @Resource
    private TCMDiagnosisService tcmDiagnosisService;

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody TCMDiagnosis diagnosis) {
        return tcmDiagnosisService.createTCMDiagnosis(diagnosis);
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody TCMDiagnosis diagnosis) {
        return tcmDiagnosisService.updateTCMDiagnosis(diagnosis);
    }

    @GetMapping("/get/{id}")
    public Map<String, Object> get(@PathVariable("id") Long id) {
        return tcmDiagnosisService.getTCMDiagnosisById(id);
    }

    @GetMapping("/latest/{patientId}")
    public Map<String, Object> getLatest(@PathVariable("patientId") Long patientId) {
        return tcmDiagnosisService.getLatestTCMDiagnosisByPatientId(patientId);
    }

    @GetMapping("/list/{patientId}")
    public Map<String, Object> getList(@PathVariable("patientId") Long patientId, @RequestParam(required = false, defaultValue = "10") int limit) {
        return tcmDiagnosisService.getTCMDiagnosisListByPatientId(patientId, limit);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable("id") Long id) {
        return tcmDiagnosisService.deleteTCMDiagnosisById(id);
    }
}