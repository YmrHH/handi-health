package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
public class PatientBrief {
    private Long id;
    private String name;
    private String phone;
    private String riskLevel;
}
