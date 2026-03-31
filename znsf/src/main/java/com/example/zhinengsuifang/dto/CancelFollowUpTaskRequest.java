package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
public class CancelFollowUpTaskRequest {
    private Long taskId;

    private String doctorUsername;
    private String doctorPassword;
}
