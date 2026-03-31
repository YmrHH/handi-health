package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyndromeAssessment {
    private Long id;

    private Long patientId;

    private String syndromeCode;

    private String syndromeName;

    private Double score;

    private Integer isStable;

    private LocalDateTime assessedAt;

    private Long createdByUserId;

    private LocalDateTime createdAt;
}
