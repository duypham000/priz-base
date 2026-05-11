package com.priz.base.application.features.auth.impl;

import com.priz.base.application.features.auth.AuthService;
import com.priz.base.application.features.auth.dto.AuthResponse;
import com.priz.base.application.features.auth.dto.ForgotPasswordRequest;
import com.priz.base.application.features.auth.dto.LoginRequest;
import com.priz.base.application.features.auth.dto.RefreshTokenRequest;
import com.priz.base.application.features.auth.dto.RegisterRequest;
import com.priz.base.application.features.auth.dto.ResetPasswordRequest;
import com.priz.base.application.features.permission.UserPermissionService;
import com.priz.common.exception.BusinessException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.jwt.JwtService;
import com.priz.base.domain.mysql_priz_base.model.AccessTokenModel;
import com.priz.base.domain.mysql_priz_base.model.RefreshTokenModel;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.AccessTokenRepository;
import com.priz.base.domain.mysql_priz_base.repository.RefreshTokenRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserPermissionService userPermissionService;

    // =============================================
    // REGISTER
    // =============================================
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL",
                    "Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_USERNAME",
                    "Username already taken");
        }

        UserModel user = UserModel.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(UserModel.Role.USER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return buildAuthResponse(user, null);
    }

    // =============================================
    // LOGIN
    // =============================================
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserModel user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                    "Account is disabled");
        }

        return buildAuthResponse(user, request.getDeviceInfo());
    }

    // =============================================
    // LOGOUT
    // =============================================
    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshTokenModel token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        accessTokenRepository.revokeAllByUserId(token.getUserId());
        token.setIsRevoked(true);
        refreshTokenRepository.save(token);
        log.info("User logged out, tokens revoked for user: {}", token.getUserId());
    }

    // =============================================
    // REFRESH TOKEN
    // =============================================
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenModel storedToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getIsRevoked()) {
            // Grace period: allow if revoked within the last 30 seconds (likely a race condition/parallel request)
            Instant revocationGraceThreshold = Instant.now().minusSeconds(30);
            if (storedToken.getUpdatedAt() != null && storedToken.getUpdatedAt().isAfter(revocationGraceThreshold)) {
                log.info("Refresh token already rotated within grace period for user: {}. This is likely a parallel request.", storedToken.getUserId());
                // In a perfect world, we'd find the NEW token created by the parallel request and return it.
                // For now, since the frontend is now locked, this is a secondary safety net.
                // We'll throw a specific message that the frontend can ignore or retry.
                throw new UnauthorizedException("Token recently rotated, please wait or use the new token");
            }
            log.warn("Refresh token revoked: {}", request.getRefreshToken());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (storedToken.isExpired()) {
            storedToken.setIsRevoked(true);
            refreshTokenRepository.save(storedToken);
            log.warn("Refresh token expired for user: {}", storedToken.getUserId());
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Mark OLD refresh token as revoked and revoke old access tokens, then generate a NEW pair
        storedToken.setIsRevoked(true);
        refreshTokenRepository.save(storedToken);
        accessTokenRepository.revokeAllByUserId(storedToken.getUserId());

        UserModel user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id",
                        storedToken.getUserId()));

        log.info("Token refreshed for user: {}", user.getEmail());
        return buildAuthResponse(user, storedToken.getDeviceInfo());
    }

    // =============================================
    // FORGOT PASSWORD
    // =============================================
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(3600));
            userRepository.save(user);

            // TODO: Tích hợp gửi email với reset link
            log.info("Password reset token generated for {}: {}", user.getEmail(), resetToken);
        });
    }

    // =============================================
    // RESET PASSWORD
    // =============================================
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserModel user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "INVALID_TOKEN", "Invalid or expired reset token"));

        if (user.getResetPasswordTokenExpiry() == null ||
                Instant.now().isAfter(user.getResetPasswordTokenExpiry())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED",
                    "Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        accessTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    // =============================================
    // PRIVATE HELPER
    // =============================================
    private AuthResponse buildAuthResponse(UserModel user, String deviceInfo) {
        String perms = userPermissionService.getEncodedPermissions(user.getId());
        String accessTokenValue = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                perms.isBlank() ? null : perms
        );

        AccessTokenModel accessToken = AccessTokenModel.builder()
                .token(accessTokenValue)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtService.getAccessTokenExpiration()))
                .isRevoked(false)
                .deviceInfo(deviceInfo)
                .build();

        accessTokenRepository.save(accessToken);

        String refreshTokenValue = jwtService.generateRefreshTokenValue();

        RefreshTokenModel refreshToken = RefreshTokenModel.builder()
                .token(refreshTokenValue)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .isRevoked(false)
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessTokenValue)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn((int) (jwtService.getAccessTokenExpiration() / 1000))
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
