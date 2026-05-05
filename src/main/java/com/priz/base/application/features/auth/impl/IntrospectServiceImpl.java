package com.priz.base.application.features.auth.impl;

import com.priz.base.application.features.auth.IntrospectService;
import com.priz.base.application.features.auth.dto.IntrospectResult;
import com.priz.base.domain.mysql_priz_base.model.AccessTokenModel;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.AccessTokenRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntrospectServiceImpl implements IntrospectService {

    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;

    @Override
    public IntrospectResult introspect(String authorizationHeader) {
        String token = extractToken(authorizationHeader);

        AccessTokenModel accessToken = accessTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid access token"));

        if (accessToken.getIsRevoked()) {
            throw new UnauthorizedException("Access token has been revoked");
        }

        if (accessToken.isExpired()) {
            throw new UnauthorizedException("Access token has expired");
        }

        UserModel user = userRepository.findById(accessToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", accessToken.getUserId()));

        if (!user.getIsActive()) {
            throw new ForbiddenException("Account is disabled");
        }

        return IntrospectResult.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Missing Authorization header");
        }
        if (!authorizationHeader.startsWith("Bearer ") && !authorizationHeader.startsWith("bearer ")) {
            throw new UnauthorizedException("Invalid Authorization format");
        }
        String token = authorizationHeader.substring(7).strip();
        if (token.isBlank()) {
            throw new UnauthorizedException("Missing token");
        }
        return token;
    }
}
