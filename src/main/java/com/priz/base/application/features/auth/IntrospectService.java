package com.priz.base.application.features.auth;

import com.priz.base.application.features.auth.dto.IntrospectResult;

public interface IntrospectService {

    /**
     * Validates an opaque access token and returns user info.
     * Checks token existence, revocation, expiry, and user.isActive.
     *
     * @param authorizationHeader full "Bearer <token>" header value
     * @return user info if token is valid and user is active
     */
    IntrospectResult introspect(String authorizationHeader);
}
