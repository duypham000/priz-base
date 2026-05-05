package com.priz.base.application.features.users.impl;

import com.priz.base.application.features.users.dto.ChangePasswordRequest;
import com.priz.base.application.features.users.dto.UpdateProfileRequest;
import com.priz.base.application.features.users.dto.UserDetailResponse;
import com.priz.common.exception.BusinessException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.RefreshTokenRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.base.testutil.SecurityTestUtil;
import com.priz.base.testutil.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        SecurityTestUtil.setSecurityContext(
                TestFixtures.TEST_USER_ID,
                TestFixtures.TEST_EMAIL,
                TestFixtures.TEST_USERNAME,
                "USER"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtil.clearSecurityContext();
    }

    // =============================================
    // GET USER DETAIL
    // =============================================

    @Test
    void getUserDetail_should_returnUserDetailResponse() {
        UserModel user = TestFixtures.createUserModel();
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));

        UserDetailResponse response = userService.getUserDetail();

        assertNotNull(response);
        assertEquals(TestFixtures.TEST_USER_ID, response.getId());
        assertEquals(TestFixtures.TEST_EMAIL, response.getEmail());
        assertEquals(TestFixtures.TEST_USERNAME, response.getUsername());
    }

    @Test
    void getUserDetail_should_throwNotFound_whenUserMissing() {
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserDetail());
    }

    // =============================================
    // UPDATE PROFILE
    // =============================================

    @Test
    void updateProfile_should_updateFieldsAndReturn() {
        UserModel user = TestFixtures.createUserModel();
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserModel.class))).thenReturn(user);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("New Name")
                .phone("0987654321")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        UserDetailResponse response = userService.updateProfile(request);

        assertNotNull(response);
        assertEquals("New Name", user.getFullName());
        assertEquals("0987654321", user.getPhone());
        assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_should_updateOnlyNonNullFields() {
        UserModel user = TestFixtures.createUserModel();
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserModel.class))).thenReturn(user);

        String originalPhone = user.getPhone();
        String originalAvatar = user.getAvatarUrl();

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Only Name Changed")
                .build();

        userService.updateProfile(request);

        assertEquals("Only Name Changed", user.getFullName());
        assertEquals(originalPhone, user.getPhone());
        assertEquals(originalAvatar, user.getAvatarUrl());
    }

    // =============================================
    // CHANGE PASSWORD
    // =============================================

    @Test
    void changePassword_should_succeed_withCorrectCurrentPassword() {
        UserModel user = TestFixtures.createUserModel();
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("currentPass")
                .newPassword("newPassword123")
                .build();

        userService.changePassword(request);

        assertEquals("encoded-new-password", user.getPassword());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(TestFixtures.TEST_USER_ID);
    }

    @Test
    void changePassword_should_throwBadRequest_whenCurrentPasswordWrong() {
        UserModel user = TestFixtures.createUserModel();
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", user.getPassword())).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPass")
                .newPassword("newPassword123")
                .build();

        assertThrows(BusinessException.class, () -> userService.changePassword(request));
    }

    // =============================================
    // DELETE ACCOUNT
    // =============================================

    @Test
    void deleteAccount_should_deactivateAndRevokeTokens() {
        UserModel user = TestFixtures.createUserModel();
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));

        userService.deleteAccount();

        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(TestFixtures.TEST_USER_ID);
    }
}
