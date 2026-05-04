package com.priz.base.infrastructure.security.aspect;

import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.jwt.JwtService;
import com.priz.common.security.SecurityContextHolder;
import com.priz.common.security.annotation.Secured;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecuredAspectTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private SecuredAspect securedAspect;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clear();
    }

    @Test
    void validateSecurity_should_proceedWithValidToken() throws Throwable {
        mockRequest.addHeader("Authorization", "Bearer valid-token");
        setupJoinPointWithMethod("unsecuredMethod");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        Claims claims = createClaims("user-1", "test@example.com", "testuser", "USER");
        when(jwtService.parseToken("valid-token")).thenReturn(claims);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertEquals("result", result);
        verify(joinPoint).proceed();
        // Context should be cleared in finally block
        assertNull(SecurityContextHolder.get());
    }

    @Test
    void validateSecurity_should_throwUnauthorized_whenNoAuthHeader() {
        setupJoinPointWithMethod("unsecuredMethod");

        assertThrows(UnauthorizedException.class,
                () -> securedAspect.validateSecurity(joinPoint));
    }

    @Test
    void validateSecurity_should_throwUnauthorized_whenNotBearerPrefix() {
        mockRequest.addHeader("Authorization", "Basic some-credentials");
        setupJoinPointWithMethod("unsecuredMethod");

        assertThrows(UnauthorizedException.class,
                () -> securedAspect.validateSecurity(joinPoint));
    }

    @Test
    void validateSecurity_should_throwUnauthorized_whenTokenInvalid() {
        mockRequest.addHeader("Authorization", "Bearer invalid-token");
        setupJoinPointWithMethod("unsecuredMethod");

        when(jwtService.validateToken("invalid-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> securedAspect.validateSecurity(joinPoint));
    }

    @Test
    void validateSecurity_should_throwForbidden_whenRoleNotAllowed() throws Throwable {
        mockRequest.addHeader("Authorization", "Bearer valid-token");
        setupJoinPointWithMethod("adminOnlyMethod");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        Claims claims = createClaims("user-1", "test@example.com", "testuser", "USER");
        when(jwtService.parseToken("valid-token")).thenReturn(claims);

        assertThrows(ForbiddenException.class,
                () -> securedAspect.validateSecurity(joinPoint));
    }

    @Test
    void validateSecurity_should_allowAccess_whenRoleMatches() throws Throwable {
        mockRequest.addHeader("Authorization", "Bearer valid-token");
        setupJoinPointWithMethod("adminOnlyMethod");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        Claims claims = createClaims("user-1", "admin@example.com", "admin", "ADMIN");
        when(jwtService.parseToken("valid-token")).thenReturn(claims);
        when(joinPoint.proceed()).thenReturn("admin-result");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertEquals("admin-result", result);
    }

    @Test
    void validateSecurity_should_clearSecurityContext_evenOnException() {
        mockRequest.addHeader("Authorization", "Bearer valid-token");
        setupJoinPointWithMethod("unsecuredMethod");

        when(jwtService.validateToken("valid-token")).thenReturn(false);

        try {
            securedAspect.validateSecurity(joinPoint);
        } catch (Throwable ignored) {
        }

        assertNull(SecurityContextHolder.get());
    }

    // =============================================
    // HELPERS
    // =============================================

    private Claims createClaims(String userId, String email, String username, String role) {
        return new DefaultClaims(Map.of(
                "sub", userId,
                "email", email,
                "username", username,
                "role", role
        ));
    }

    private void setupJoinPointWithMethod(String methodName) {
        try {
            Method method = TestMethods.class.getMethod(methodName);
            lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
            lenient().when(methodSignature.getMethod()).thenReturn(method);
            lenient().when(joinPoint.getTarget()).thenReturn(new TestMethods());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dummy class with annotated methods for testing annotation resolution.
     */
    public static class TestMethods {
        @Secured
        public void unsecuredMethod() {}

        @Secured(roles = "ADMIN")
        public void adminOnlyMethod() {}
    }
}
