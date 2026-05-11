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
        name = "user_permission_group",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_permission_group",
                        columnNames = {"user_id", "permission_group_code"}
                )
        },
        indexes = {
                @Index(name = "idx_upg_user_id", columnList = "user_id"),
                @Index(name = "idx_upg_group_code", columnList = "permission_group_code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionGroupModel extends BaseModel {

    @AdminRelation(name = "user", targetResource = "users", displayLabel = "User")
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "permission_group_code", nullable = false, length = 100)
    private String permissionGroupCode;
}
