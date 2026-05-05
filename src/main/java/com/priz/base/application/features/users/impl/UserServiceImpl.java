package com.priz.base.application.features.users.impl;

import com.priz.base.application.features.users.UserService;
import com.priz.base.application.features.users.converter.UserConverter;
import com.priz.base.application.features.users.dto.ChangePasswordRequest;
import com.priz.base.application.features.users.dto.UpdateProfileRequest;
import com.priz.base.application.features.users.dto.UserDetailResponse;
import com.priz.common.exception.BusinessException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.RefreshTokenRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.common.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // =============================================
    // GET USER DETAIL
    // =============================================
    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail() {
        String userId = SecurityContextHolder.getCurrentUserId();
        UserModel user = findUserById(userId);
        return UserConverter.toDetailResponse(user);
    }

    // =============================================
    // UPDATE PROFILE
    // =============================================
    @Override
    @Transactional
    public UserDetailResponse updateProfile(UpdateProfileRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        UserModel user = findUserById(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return UserConverter.toDetailResponse(user);
    }

    // =============================================
    // CHANGE PASSWORD
    // =============================================
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String userId = SecurityContextHolder.getCurrentUserId();
        UserModel user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Password changed for user: {}", userId);
    }

    // =============================================
    // DELETE ACCOUNT
    // =============================================
    @Override
    @Transactional
    public void deleteAccount() {
        String userId = SecurityContextHolder.getCurrentUserId();
        UserModel user = findUserById(userId);

        user.setIsActive(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Account deactivated for user: {}", userId);
    }

    // =============================================
    // PRIVATE HELPER
    // =============================================

    private UserModel findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
