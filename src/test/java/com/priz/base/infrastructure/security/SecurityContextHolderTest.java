package com.priz.base.infrastructure.security;

import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHolderTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clear();
    }

    @Test
    void set_and_get_should_returnSameContext() {
        SecurityContext context = SecurityContext.builder()
                .userId("user-1")
                .email("test@example.com")
                .username("testuser")
                .role("USER")
                .build();

        SecurityContextHolder.set(context);

        assertSame(context, SecurityContextHolder.get());
    }

    @Test
    void get_should_returnNull_whenNotSet() {
        assertNull(SecurityContextHolder.get());
    }

    @Test
    void clear_should_removeContext() {
        SecurityContext context = SecurityContext.builder()
                .userId("user-1")
                .build();
        SecurityContextHolder.set(context);

        SecurityContextHolder.clear();

        assertNull(SecurityContextHolder.get());
    }

    @Test
    void getCurrentUserId_should_returnUserId() {
        SecurityContext context = SecurityContext.builder()
                .userId("user-123")
                .build();
        SecurityContextHolder.set(context);

        assertEquals("user-123", SecurityContextHolder.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_should_throwIllegalState_whenNoContext() {
        assertThrows(IllegalStateException.class, SecurityContextHolder::getCurrentUserId);
    }

    @Test
    void getCurrentEmail_should_returnEmail() {
        SecurityContext context = SecurityContext.builder()
                .email("test@example.com")
                .build();
        SecurityContextHolder.set(context);

        assertEquals("test@example.com", SecurityContextHolder.getCurrentEmail());
    }

    @Test
    void getCurrentEmail_should_throwIllegalState_whenNoContext() {
        assertThrows(IllegalStateException.class, SecurityContextHolder::getCurrentEmail);
    }

    @Test
    void getCurrentRole_should_returnRole() {
        SecurityContext context = SecurityContext.builder()
                .role("ADMIN")
                .build();
        SecurityContextHolder.set(context);

        assertEquals("ADMIN", SecurityContextHolder.getCurrentRole());
    }

    @Test
    void getCurrentRole_should_throwIllegalState_whenNoContext() {
        assertThrows(IllegalStateException.class, SecurityContextHolder::getCurrentRole);
    }
}
