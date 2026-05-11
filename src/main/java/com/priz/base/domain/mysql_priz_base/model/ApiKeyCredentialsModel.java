package com.priz.base.domain.mysql_priz_base.model;

import com.priz.base.common.model.BaseModel;
import com.priz.common.admin.annotation.AdminHidden;
import com.priz.common.admin.annotation.AdminManaged;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AdminManaged
@Entity
@Table(
        name = "api_key_credentials",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_key_credentials_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_api_key_status", columnList = "status"),
                @Index(name = "idx_api_key_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCredentialsModel extends BaseModel {

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @AdminHidden
    @Column(name = "key_hash", nullable = false, length = 100)
    private String keyHash;

    @AdminHidden
    @Column(name = "secret_key_encrypted", length = 500)
    private String secretKeyEncrypted;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "allow_ips", length = 1000)
    private String allowIps;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
