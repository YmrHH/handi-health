package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
/**
 * 候選患者返返 DTO。
 */
public class CandidatePatient {
    private Long patientId;
    private String reason;
    private Long abnormalCount;
    private Long days;
}

