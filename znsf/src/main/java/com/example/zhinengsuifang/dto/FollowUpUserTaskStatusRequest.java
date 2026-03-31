package com.example.zhinengsuifang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FollowUpUserTaskStatusRequest {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private String followUpUsername;

    private String followUpPassword;
}
