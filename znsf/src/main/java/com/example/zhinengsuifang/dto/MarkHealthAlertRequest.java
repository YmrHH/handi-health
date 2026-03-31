package com.example.zhinengsuifang.dto;

import lombok.Data;

@Data
/**
 * 标记健康预警状态请求 DTO。
 */
public class MarkHealthAlertRequest {
    private String doctorUsername;
    private String doctorPassword;

    private Long alertId;

    private String status;
}

