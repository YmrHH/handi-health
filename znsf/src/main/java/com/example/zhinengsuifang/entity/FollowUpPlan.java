package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FollowUpPlan {
    private Long id;

    private Long patientId;

    private String frequencyType;

    private Integer frequencyValue;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;

    private Long createdByDoctorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
