package com.priz.base.testutil;

import com.priz.base.application.features.auth.dto.LoginRequest;
import com.priz.base.application.features.auth.dto.RegisterRequest;
import com.priz.base.domain.mysql_priz_base.model.FileModel;
import com.priz.base.domain.mysql_priz_base.model.RefreshTokenModel;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.common.security.SecurityContext;

import java.time.Instant;
import java.util.UUID;

public final class TestFixtures {

    public static final String TEST_USER_ID = "test-user-id-001";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_PASSWORD = "password123";
    public static final String TEST_ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";
    public static final String TEST_FULL_NAME = "Test User";
    public static final String TEST_PHONE = "0123456789";
    public static final String TEST_ACCESS_TOKEN = "test-access-token";
    public static final String TEST_REFRESH_TOKEN = "test-refresh-token";

    private TestFixtures() {}

    public static UserModel createUserModel() {
        UserModel user = UserModel.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(TEST_ENCODED_PASSWORD)
                .fullName(TEST_FULL_NAME)
                .phone(TEST_PHONE)
                .role(UserModel.Role.USER)
                .isActive(true)
                .build();
        user.setId(TEST_USER_ID);
        user.setCreatedAt(Instant.now());
        return user;
    }

    public static UserModel createUserModel(String id, String email, UserModel.Role role) {
        UserModel user = UserModel.builder()
                .username("user-" + id)
                .email(email)
                .password(TEST_ENCODED_PASSWORD)
                .fullName("User " + id)
                .role(role)
                .isActive(true)
                .build();
        user.setId(id);
        user.setCreatedAt(Instant.now());
        return user;
    }

    public static UserModel createAdminUserModel() {
        UserModel user = createUserModel();
        user.setRole(UserModel.Role.ADMIN);
        return user;
    }

    public static RegisterRequest createRegisterRequest() {
        return RegisterRequest.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .fullName(TEST_FULL_NAME)
                .phone(TEST_PHONE)
                .build();
    }

    public static LoginRequest createLoginRequest() {
        return LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .deviceInfo("Test Device")
                .build();
    }

    public static RefreshTokenModel createRefreshTokenModel(String userId, boolean revoked, boolean expired) {
        RefreshTokenModel token = RefreshTokenModel.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiresAt(expired ? Instant.now().minusSeconds(3600) : Instant.now().plusSeconds(604800))
                .isRevoked(revoked)
                .deviceInfo("Test Device")
                .build();
        token.setId(UUID.randomUUID().toString());
        return token;
    }

    public static FileModel createFileModel(String userId) {
        FileModel file = FileModel.builder()
                .originalName("test-file.txt")
                .storedName("stored-" + UUID.randomUUID())
                .filePath("/uploads/stored-file.txt")
                .fileType("txt")
                .fileSize(1024L)
                .contentType("text/plain")
                .userId(userId)
                .description("Test file")
                .isSynced(false)
                .build();
        file.setId(UUID.randomUUID().toString());
        file.setCreatedAt(Instant.now());
        return file;
    }

    public static SecurityContext createSecurityContext(String userId, String role) {
        return SecurityContext.builder()
                .userId(userId)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .role(role)
                .build();
    }
}
