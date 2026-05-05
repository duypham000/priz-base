package com.priz.base.application.features.auth.impl;

import com.priz.base.application.features.auth.dto.*;
import com.priz.common.exception.BusinessException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.jwt.JwtService;
import com.priz.base.domain.mysql_priz_base.model.RefreshTokenModel;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.model.AccessTokenModel;
import com.priz.base.domain.mysql_priz_base.repository.RefreshTokenRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.base.domain.mysql_priz_base.repository.AccessTokenRepository;
import com.priz.base.testutil.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessTokenRepository accessTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    // =============================================
    // REGISTER
    // =============================================

    @Test
    void register_should_returnAuthResponse_withValidRequest() {
        RegisterRequest request = TestFixtures.createRegisterRequest();
        UserModel savedUser = TestFixtures.createUserModel();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn(TestFixtures.TEST_ENCODED_PASSWORD);
        when(userRepository.save(any(UserModel.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(TestFixtures.TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshTokenValue()).thenReturn(TestFixtures.TEST_REFRESH_TOKEN);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(TestFixtures.TEST_ACCESS_TOKEN, response.getAccessToken());
        assertEquals(TestFixtures.TEST_REFRESH_TOKEN, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(TestFixtures.TEST_EMAIL, response.getUser().getEmail());
        verify(userRepository).save(any(UserModel.class));
        verify(refreshTokenRepository).save(any(RefreshTokenModel.class));
    }

    @Test
    void register_should_throwConflict_whenEmailExists() {
        RegisterRequest request = TestFixtures.createRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void register_should_throwConflict_whenUsernameExists() {
        RegisterRequest request = TestFixtures.createRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals("Username already taken", ex.getMessage());
    }

    // =============================================
    // LOGIN
    // =============================================

    @Test
    void login_should_returnAuthResponse_withValidCredentials() {
        LoginRequest request = TestFixtures.createLoginRequest();
        UserModel user = TestFixtures.createUserModel();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(TestFixtures.TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshTokenValue()).thenReturn(TestFixtures.TEST_REFRESH_TOKEN);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(TestFixtures.TEST_ACCESS_TOKEN, response.getAccessToken());
    }

    @Test
    void login_should_throwUnauthorized_whenEmailNotFound() {
        LoginRequest request = TestFixtures.createLoginRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_should_throwUnauthorized_whenPasswordWrong() {
        LoginRequest request = TestFixtures.createLoginRequest();
        UserModel user = TestFixtures.createUserModel();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_should_throwForbidden_whenAccountDisabled() {
        LoginRequest request = TestFixtures.createLoginRequest();
        UserModel user = TestFixtures.createUserModel();
        user.setIsActive(false);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("Account is disabled", ex.getMessage());
    }

    // =============================================
    // LOGOUT
    // =============================================

    @Test
    void logout_should_revokeRefreshToken() {
        RefreshTokenModel token = TestFixtures.createRefreshTokenModel(TestFixtures.TEST_USER_ID, false, false);

        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        authService.logout(token.getToken());

        assertTrue(token.getIsRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logout_should_throwNotFound_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.logout("invalid-token"));
    }

    // =============================================
    // REFRESH TOKEN
    // =============================================

    @Test
    void refreshToken_should_returnNewAuthResponse() {
        RefreshTokenModel storedToken = TestFixtures.createRefreshTokenModel(TestFixtures.TEST_USER_ID, false, false);
        UserModel user = TestFixtures.createUserModel();
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(storedToken.getToken())
                .build();

        when(refreshTokenRepository.findByToken(storedToken.getToken())).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(TestFixtures.TEST_USER_ID)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("new-access-token");
        when(jwtService.generateRefreshTokenValue()).thenReturn("new-refresh-token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertTrue(storedToken.getIsRevoked());
    }

    @Test
    void refreshToken_should_throwUnauthorized_whenTokenRevoked() {
        RefreshTokenModel storedToken = TestFixtures.createRefreshTokenModel(TestFixtures.TEST_USER_ID, true, false);
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(storedToken.getToken())
                .build();

        when(refreshTokenRepository.findByToken(storedToken.getToken())).thenReturn(Optional.of(storedToken));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_should_throwUnauthorized_whenTokenExpired() {
        RefreshTokenModel storedToken = TestFixtures.createRefreshTokenModel(TestFixtures.TEST_USER_ID, false, true);
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(storedToken.getToken())
                .build();

        when(refreshTokenRepository.findByToken(storedToken.getToken())).thenReturn(Optional.of(storedToken));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_should_throwUnauthorized_whenTokenInvalid() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
    }

    // =============================================
    // FORGOT PASSWORD
    // =============================================

    @Test
    void forgotPassword_should_setResetToken_whenUserExists() {
        UserModel user = TestFixtures.createUserModel();
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(TestFixtures.TEST_EMAIL)
                .build();

        when(userRepository.findByEmail(TestFixtures.TEST_EMAIL)).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        assertNotNull(user.getResetPasswordToken());
        assertNotNull(user.getResetPasswordTokenExpiry());
        verify(userRepository).save(user);
    }

    @Test
    void forgotPassword_should_doNothing_whenUserNotFound() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@example.com")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(userRepository, never()).save(any());
    }

    // =============================================
    // RESET PASSWORD
    // =============================================

    @Test
    void resetPassword_should_updatePasswordAndRevokeTokens() {
        UserModel user = TestFixtures.createUserModel();
        user.setResetPasswordToken("valid-reset-token");
        user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(3600));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-reset-token")
                .newPassword("newPassword123")
                .build();

        when(userRepository.findByResetPasswordToken("valid-reset-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");

        authService.resetPassword(request);

        assertEquals("encoded-new-password", user.getPassword());
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordTokenExpiry());
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    void resetPassword_should_throwBadRequest_whenTokenInvalid() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("newPassword123")
                .build();

        when(userRepository.findByResetPasswordToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_should_throwBadRequest_whenTokenExpired() {
        UserModel user = TestFixtures.createUserModel();
        user.setResetPasswordToken("expired-token");
        user.setResetPasswordTokenExpiry(Instant.now().minusSeconds(3600));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("newPassword123")
                .build();

        when(userRepository.findByResetPasswordToken("expired-token")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.resetPassword(request));
    }
}
