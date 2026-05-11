package com.priz.base.infrastructure.security.aspect;

import com.priz.base.infrastructure.security.apikey.ApiKeyAuthenticationFilter;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.ApiKeyPrincipal;
import com.priz.common.security.annotation.ApiKeySecured;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeySecuredAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private ApiKeySecuredAspect aspect;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        aspect = new ApiKeySecuredAspect();
        mockRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ---- No principal ----

    @Test
    void validateApiKey_noPrincipal_throwsUnauthorized() {
        setupJoinPoint("scopedEndpoint");

        assertThatThrownBy(() -> aspect.validateApiKey(joinPoint))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("API key required");
    }

    // ---- Principal present, scope check ----

    @Test
    void validateApiKey_hasRequiredScope_proceeds() throws Throwable {
        setupJoinPoint("scopedEndpoint");
        setPrincipal(Set.of("read:base:file", "manage:base:api_key"), false);
        when(joinPoint.proceed()).thenReturn("granted");

        Object result = aspect.validateApiKey(joinPoint);

        assertThat(result).isEqualTo("granted");
    }

    @Test
    void validateApiKey_missingRequiredScope_throwsForbidden() {
        setupJoinPoint("scopedEndpoint");
        setPrincipal(Set.of("read:base:other"), false);

        assertThatThrownBy(() -> aspect.validateApiKey(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Insufficient scope");
    }

    // ---- requireAllScopes ----

    @Test
    void validateApiKey_allScopesPresent_proceeds() throws Throwable {
        setupJoinPoint("allScopesEndpoint");
        setPrincipal(Set.of("read:base:file", "write:base:file"), false);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.validateApiKey(joinPoint);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void validateApiKey_missingOneOfAllScopes_throwsForbidden() {
        setupJoinPoint("allScopesEndpoint");
        setPrincipal(Set.of("read:base:file"), false); // missing write:base:file

        assertThatThrownBy(() -> aspect.validateApiKey(joinPoint))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- requireSignature ----

    @Test
    void validateApiKey_signatureRequired_andVerified_proceeds() throws Throwable {
        setupJoinPoint("signedEndpoint");
        setPrincipal(Set.of("manage:base:api_key"), true); // signatureVerified = true
        when(joinPoint.proceed()).thenReturn("signed");

        Object result = aspect.validateApiKey(joinPoint);

        assertThat(result).isEqualTo("signed");
    }

    @Test
    void validateApiKey_signatureRequired_butNotVerified_throwsUnauthorized() {
        setupJoinPoint("signedEndpoint");
        setPrincipal(Set.of("manage:base:api_key"), false); // signatureVerified = false

        assertThatThrownBy(() -> aspect.validateApiKey(joinPoint))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("signature required");
    }

    // ---- No scopes declared = any valid key passes ----

    @Test
    void validateApiKey_noScopesDeclared_anyValidKeyPasses() throws Throwable {
        setupJoinPoint("openEndpoint");
        setPrincipal(Set.of(), false);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.validateApiKey(joinPoint);

        assertThat(result).isEqualTo("ok");
    }

    // ---- helpers ----

    private void setPrincipal(Set<String> permissions, boolean signatureVerified) {
        ApiKeyPrincipal principal = ApiKeyPrincipal.builder()
                .apiKeyId("key-id-1")
                .code("abc123code")
                .name("Test Key")
                .permissions(permissions)
                .signatureVerified(signatureVerified)
                .build();
        mockRequest.setAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL, principal);
    }

    private void setupJoinPoint(String methodName) {
        try {
            Method method = TestEndpoints.class.getMethod(methodName);
            lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
            lenient().when(methodSignature.getMethod()).thenReturn(method);
            lenient().when(joinPoint.getTarget()).thenReturn(new TestEndpoints());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestEndpoints {
        @ApiKeySecured(scopes = "read:base:file")
        public void scopedEndpoint() {}

        @ApiKeySecured(scopes = {"read:base:file", "write:base:file"}, requireAllScopes = true)
        public void allScopesEndpoint() {}

        @ApiKeySecured(scopes = "manage:base:api_key", requireSignature = true)
        public void signedEndpoint() {}

        @ApiKeySecured
        public void openEndpoint() {}
    }
}
