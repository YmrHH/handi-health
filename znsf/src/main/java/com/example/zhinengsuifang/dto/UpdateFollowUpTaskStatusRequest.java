package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
/**
 * 更新随访任务状态请求 DTO。
 */
public class UpdateFollowUpTaskStatusRequest {
    private Long taskId;

    private String doctorUsername;
    private String doctorPassword;

    private String status;
}
