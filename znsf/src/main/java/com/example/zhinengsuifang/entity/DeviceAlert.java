package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceAlert {
    private Long id;

    private Long patientId;

    private String deviceSn;

    private String alertCode;

    private String alertMessage;

    private String severity;

    private String status;

    private LocalDateTime occurredAt;

    private LocalDateTime createdAt;
}
