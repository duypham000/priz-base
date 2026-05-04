package com.priz.base.domain.mysql.priz_base.model;

import com.priz.common.admin.annotation.AdminHidden;
import com.priz.common.admin.annotation.AdminManaged;
import com.priz.base.common.model.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AdminManaged
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "username")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModel extends BaseModel {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @AdminHidden
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @AdminHidden
    @Column(name = "reset_password_token", length = 100)
    private String resetPasswordToken;

    @AdminHidden
    @Column(name = "reset_password_token_expiry")
    private Instant resetPasswordTokenExpiry;

    public enum Role {
        USER, ADMIN
    }
}
