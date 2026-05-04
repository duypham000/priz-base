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
public class AsyncUploadResponse {

    private String jobId;
    private String fileId;
    private String status;
    private String message;
    private Instant createdAt;
}
