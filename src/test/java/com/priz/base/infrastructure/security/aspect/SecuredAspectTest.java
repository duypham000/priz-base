package com.priz.base.infrastructure.security.aspect;

import com.priz.base.config.security.JwtAuthenticationFilter;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.SecurityContextHolder;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.permission.PermissionAction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecuredAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private SecuredAspect securedAspect;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        securedAspect = new SecuredAspect();
        ReflectionTestUtils.setField(securedAspect, "applicationName", "base");
        mockRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clear();
    }

    // ---- Unauthorized (no userId) ----

    @Test
    void validateSecurity_missingUserId_throwsUnauthorized() {
        setupJoinPoint("anyAuthenticated");

        assertThatThrownBy(() -> securedAspect.validateSecurity(joinPoint))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---- Authenticated, no role/permission restrictions ----

    @Test
    void validateSecurity_authenticatedNoRestrictions_proceeds() throws Throwable {
        setupJoinPoint("anyAuthenticated");
        setRequestAttributes("user-1", List.of("USER"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void validateSecurity_securityContextCleared_afterProceed() throws Throwable {
        setupJoinPoint("anyAuthenticated");
        setRequestAttributes("user-1", List.of("USER"));
        when(joinPoint.proceed()).thenReturn("ok");

        securedAspect.validateSecurity(joinPoint);

        assertThat(SecurityContextHolder.get()).isNull();
    }

    // ---- Role checks ----

    @Test
    void validateSecurity_correctRole_proceeds() throws Throwable {
        setupJoinPoint("adminOnly");
        setRequestAttributes("admin-1", List.of("ADMIN"));
        when(joinPoint.proceed()).thenReturn("admin-result");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertThat(result).isEqualTo("admin-result");
    }

    @Test
    void validateSecurity_wrongRole_throwsForbidden() {
        setupJoinPoint("adminOnly");
        setRequestAttributes("user-1", List.of("USER"));

        assertThatThrownBy(() -> securedAspect.validateSecurity(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ADMIN");
    }

    // ---- Bitmask path: permissions[] ----

    @Test
    void validateSecurity_bitmaskReadAction_userHasRead_proceeds() throws Throwable {
        setupJoinPoint("bitmaskRead");
        setRequestAttributes("user-1", List.of("USER"));
        // READ mask = 2; key = "base:article"
        mockRequest.setAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP,
                Map.of("base:article", 2));
        when(joinPoint.proceed()).thenReturn("read-ok");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertThat(result).isEqualTo("read-ok");
    }

    @Test
    void validateSecurity_bitmaskReadAction_userMissingRead_throwsForbidden() {
        setupJoinPoint("bitmaskRead");
        setRequestAttributes("user-1", List.of("USER"));
        // user has only CREATE (1), not READ (2)
        mockRequest.setAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP,
                Map.of("base:article", 1));

        assertThatThrownBy(() -> securedAspect.validateSecurity(joinPoint))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validateSecurity_bitmaskNoPermsMap_throwsForbidden() {
        setupJoinPoint("bitmaskRead");
        setRequestAttributes("user-1", List.of("USER"));
        // No permsMap attribute set

        assertThatThrownBy(() -> securedAspect.validateSecurity(joinPoint))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validateSecurity_bitmaskGlobalKey_usesGlobalPrefix() throws Throwable {
        setupJoinPoint("bitmaskGlobal");
        setRequestAttributes("user-1", List.of("USER"));
        // isGlobal=true, customKey="report" → key = "global:report"; UPDATE mask = 4
        mockRequest.setAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP,
                Map.of("global:report", 4));
        when(joinPoint.proceed()).thenReturn("global-ok");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertThat(result).isEqualTo("global-ok");
    }

    @Test
    void validateSecurity_bitmaskImplicitKey_stripsControllerSuffix() throws Throwable {
        setupJoinPoint("bitmaskImplicit");
        setRequestAttributes("user-1", List.of("USER"));
        // no customKey → class TestMethods has no Controller/Service suffix → key = "base:testmethods"
        mockRequest.setAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP,
                Map.of("base:testmethods", 2));
        when(joinPoint.proceed()).thenReturn("implicit-ok");

        Object result = securedAspect.validateSecurity(joinPoint);

        assertThat(result).isEqualTo("implicit-ok");
    }

    // ---- SecurityContext is cleared even on exception ----

    @Test
    void validateSecurity_contextClearedEvenWhenProceedThrows() {
        setupJoinPoint("anyAuthenticated");
        setRequestAttributes("user-1", List.of("USER"));
        try {
            when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));
            securedAspect.validateSecurity(joinPoint);
        } catch (Throwable ignored) {}

        assertThat(SecurityContextHolder.get()).isNull();
    }

    // ---- helpers ----

    private void setRequestAttributes(String userId, List<String> roles) {
        mockRequest.setAttribute(JwtAuthenticationFilter.ATTR_USER_ID, userId);
        mockRequest.setAttribute(JwtAuthenticationFilter.ATTR_ROLES, roles);
    }

    private void setupJoinPoint(String methodName) {
        try {
            Method method = TestMethods.class.getMethod(methodName);
            lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
            lenient().when(methodSignature.getMethod()).thenReturn(method);
            lenient().when(joinPoint.getTarget()).thenReturn(new TestMethods());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestMethods {
        @Secured
        public void anyAuthenticated() {}

        @Secured(roles = "ADMIN")
        public void adminOnly() {}

        @Secured(permissions = {PermissionAction.READ}, customKey = "article")
        public void bitmaskRead() {}

        @Secured(permissions = {PermissionAction.UPDATE}, customKey = "report", isGlobal = true)
        public void bitmaskGlobal() {}

        @Secured(permissions = {PermissionAction.READ})
        public void bitmaskImplicit() {}
    }
}
