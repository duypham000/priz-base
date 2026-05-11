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
        name = "permission",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_permission_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_permission_custom_key_mask", columnNames = {"custom_key", "action_mask"})
        },
        indexes = {
                @Index(name = "idx_permission_status", columnList = "status"),
                @Index(name = "idx_permission_group_code", columnList = "group_code"),
                @Index(name = "idx_permission_custom_key", columnList = "custom_key")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionModel extends BaseModel {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "group_code", length = 100)
    private String groupCode;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Bitmask resource key. Ví dụ: "base:article_manager", "global:report".
     * Kết hợp với actionMask tạo thành một permission bitmask đơn vị.
     */
    @Column(name = "custom_key", length = 150)
    private String customKey;

    /**
     * Bitmask giá trị (1=CREATE, 2=READ, 4=UPDATE, 8=DELETE, 16=SEARCH).
     */
    @Column(name = "action_mask")
    @Builder.Default
    private int actionMask = 0;
}
