package com.example.zhinengsuifang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RescheduleFollowUpTaskRequest {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private String followUpUsername;

    private String followUpPassword;

    @NotBlank(message = "dueAt 不能为空")
    private String dueAt;
}
