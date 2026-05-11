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
        name = "permission_group_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_group_mapping",
                        columnNames = {"permission_group_code", "permission_code"}
                )
        },
        indexes = {
                @Index(name = "idx_pgm_group_code", columnList = "permission_group_code"),
                @Index(name = "idx_pgm_permission_code", columnList = "permission_code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupMappingModel extends BaseModel {

    @Column(name = "permission_group_code", nullable = false, length = 100)
    private String permissionGroupCode;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;
}
