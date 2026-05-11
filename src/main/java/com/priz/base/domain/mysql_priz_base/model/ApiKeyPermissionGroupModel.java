package com.priz.base.domain.mysql_priz_base.model;

import com.priz.base.common.model.BaseModel;
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

@AdminManaged
@Entity
@Table(
        name = "api_key_permission_group",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_key_permission_group_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_akpg_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyPermissionGroupModel extends BaseModel {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
