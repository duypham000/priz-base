package com.priz.base.application.features.files.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailResponse {

    private String id;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String contentType;
    private String description;
    private String downloadUrl;
    private Boolean isSynced;
    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
}
