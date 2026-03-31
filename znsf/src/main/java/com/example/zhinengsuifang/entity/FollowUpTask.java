package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 随访任务（派单）实体。
 */
public class FollowUpTask {
    private Long id;

    private Long patientId;
    private Long doctorId;
    private Long followUpUserId;

    private String triggerType;
    private String description;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String ext1; // planDate 计划随访日期
    private String ext2; // checklist JSON 随访检查项
    private String ext3;
    private String ext4;
    private String ext5;
}

