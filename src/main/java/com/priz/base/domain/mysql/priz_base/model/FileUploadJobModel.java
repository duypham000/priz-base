package com.priz.base.domain.mysql.priz_base.model;

import com.priz.base.common.model.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_upload_jobs", indexes = {
        @Index(name = "idx_file_upload_jobs_file_id", columnList = "file_id"),
        @Index(name = "idx_file_upload_jobs_status", columnList = "status"),
        @Index(name = "idx_file_upload_jobs_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadJobModel extends BaseModel {

    public enum Status {
        PENDING, UPLOADING, COMPLETED, FAILED
    }

    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "telegram_url", length = 500)
    private String telegramUrl;

    @Column(name = "telegram_message_id")
    private Long telegramMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
}
