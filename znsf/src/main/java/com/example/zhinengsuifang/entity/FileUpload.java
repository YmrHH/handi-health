package com.example.zhinengsuifang.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileUpload {
    private Long id;
    private String bizType;
    private Long patientId;
    private Long uploaderUserId;
    private String originalName;
    private String fileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String storagePath;
    private String url;
    private String sha1;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
}
