package com.priz.base.application.features.auth;

import com.priz.base.application.features.auth.dto.AuthResponse;
import com.priz.base.application.features.auth.dto.ForgotPasswordRequest;
import com.priz.base.application.features.auth.dto.LoginRequest;
import com.priz.base.application.features.auth.dto.RefreshTokenRequest;
import com.priz.base.application.features.auth.dto.RegisterRequest;
import com.priz.base.application.features.auth.dto.ResetPasswordRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String refreshToken);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
