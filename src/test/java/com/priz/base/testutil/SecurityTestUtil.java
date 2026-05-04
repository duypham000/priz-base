package com.priz.base.testutil;

import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;

public final class SecurityTestUtil {

    private SecurityTestUtil() {}

    public static void setSecurityContext(String userId, String email, String username, String role) {
        SecurityContext context = SecurityContext.builder()
                .userId(userId)
                .email(email)
                .username(username)
                .role(role)
                .build();
        SecurityContextHolder.set(context);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clear();
    }
}
