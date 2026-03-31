package com.example.zhinengsuifang.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepublishFollowUpTaskRequest {
    private Long taskId;

    private String doctorUsername;
    private String doctorPassword;

    private Long followUpUserId;

    private String triggerType;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dueAt;
}
