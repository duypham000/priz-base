package com.priz.base.application.features.users.converter;

import com.priz.base.application.features.users.dto.UserDetailResponse;
import com.priz.base.domain.mysql_priz_base.model.UserModel;

public final class UserConverter {

    private UserConverter() {}

    public static UserDetailResponse toDetailResponse(UserModel model) {
        return UserDetailResponse.builder()
                .id(model.getId())
                .username(model.getUsername())
                .email(model.getEmail())
                .fullName(model.getFullName())
                .phone(model.getPhone())
                .avatarUrl(model.getAvatarUrl())
                .role(model.getRole().name())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
