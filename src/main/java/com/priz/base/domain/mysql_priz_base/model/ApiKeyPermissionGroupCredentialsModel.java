package com.priz.base.domain.mysql_priz_base.model;

import com.priz.base.common.model.BaseModel;
import com.priz.common.admin.annotation.AdminManaged;
import com.priz.common.admin.annotation.AdminRelation;
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

@AdminManaged
@Entity
@Table(
        name = "api_key_permission_group_credentials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_akpgc",
                        columnNames = {"api_key_credentials_id", "permission_group_code"}
                )
        },
        indexes = {
                @Index(name = "idx_akpgc_credentials_id", columnList = "api_key_credentials_id"),
                @Index(name = "idx_akpgc_group_code", columnList = "permission_group_code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyPermissionGroupCredentialsModel extends BaseModel {

    @AdminRelation(name = "credentials", targetResource = "api-key-credentials", displayLabel = "API Key")
    @Column(name = "api_key_credentials_id", nullable = false, length = 36)
    private String apiKeyCredentialsId;

    @Column(name = "permission_group_code", nullable = false, length = 100)
    private String permissionGroupCode;
}
