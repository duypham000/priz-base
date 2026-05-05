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
public class FileProcessEvent {

    private String jobId;
    private String fileId;
    private String userId;
    private String storedName;       // null if sync (url-based)
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String sourceUrl;        // null if local upload
    private String description;
    private String fileContent;      // Pre-read content for ES indexing (txt/md)
    private OperationType operation; // UPLOAD, SYNC, DELETE, UPDATE
    private String traceId;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum OperationType {
        UPLOAD, SYNC, DELETE, UPDATE
    }
}
