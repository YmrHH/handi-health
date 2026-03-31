package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
public class UpdateVisitPlanStatusRequest {
    private String doctorUsername;
    private String doctorPassword;
    private Long planId;
    private String newStatus;
}
