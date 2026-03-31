package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowUpSchedule {
    private Long id;

    private Long patientId;

    private Long planId;

    private LocalDateTime dueAt;

    private String status;

    private LocalDateTime completedAt;

    private Long relatedTaskId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
