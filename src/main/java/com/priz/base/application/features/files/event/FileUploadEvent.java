package com.priz.base.application.features.files.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadEvent {

    private String jobId;
    private String fileId;
    private String userId;
    private String storedName;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String traceId;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
