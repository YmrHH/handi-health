package com.example.zhinengsuifang.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 由预警生成随访单的请求 DTO。
 */
public class CreateFollowUpFromAlertRequest {
    private String doctorUsername;
    private String doctorPassword;

    private Long alertId;
    private Long followUpUserId;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dueAt;
}

