package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationAuditLog {
    private Long id;

    private Long operatorId;

    private String operatorName;

    private String module;

    private String action;

    private String target;

    private String requestMethod;

    private String requestUri;

    private String requestQuery;

    private String requestBody;

    private Integer success;

    private Integer statusCode;

    private Long durationMs;

    private String errorMessage;

    private LocalDateTime createdAt;
}
