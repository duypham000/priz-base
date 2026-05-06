package com.priz.base.domain.mysql_priz_base.model;

import com.priz.common.admin.annotation.AdminManaged;
import com.priz.common.admin.annotation.AdminRelation;
import com.priz.base.common.model.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@AdminManaged
@Entity
@Table(name = "files", indexes = {
        @Index(name = "idx_files_user_id", columnList = "user_id"),
        @Index(name = "idx_files_file_type", columnList = "file_type"),
        @Index(name = "idx_files_original_name", columnList = "original_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileModel extends BaseModel {

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @AdminRelation(name = "user", targetResource = "users", displayLabel = "User")
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "is_synced", nullable = false)
    @Builder.Default
    private Boolean isSynced = false;
}
