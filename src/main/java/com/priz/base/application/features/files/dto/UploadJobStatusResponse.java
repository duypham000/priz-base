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
public class UploadJobStatusResponse {

    private String jobId;
    private String fileId;
    private String userId;
    private String status;
    private String telegramUrl;
    private String errorMessage;
    private Integer retryCount;
    private Integer kafkaPartition;
    private Long kafkaOffset;
    private Instant createdAt;
    private Instant updatedAt;
}
