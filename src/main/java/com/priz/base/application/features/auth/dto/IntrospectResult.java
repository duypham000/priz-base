package com.priz.base.application.features.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IntrospectResult {
    private String userId;
    private String email;
    private String username;
    private String role;
}
