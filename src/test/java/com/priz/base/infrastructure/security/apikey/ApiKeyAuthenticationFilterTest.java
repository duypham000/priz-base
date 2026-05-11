package com.priz.base.infrastructure.security.apikey;

import com.priz.base.application.features.apikey.ApiKeyAuthService;
import com.priz.base.application.features.apikey.dto.ApiKeyAuthResult;
import com.priz.common.security.ApiKeyPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyAuthService apiKeyAuthService;

    private ApiKeyAuthenticationFilter filter;

    private static final String VALID_API_KEY = "code123456789.rawKeyValue";
    private static final String CRED_ID = "cred-1";

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyAuthService);
    }

    // ---- No API key header ----

    @Test
    void doFilter_noApiKeyHeader_passesThroughWithoutCallingAuthService() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL)).isNull();
        verify(apiKeyAuthService, never()).validate(any(), any(), any(), any(), anyLong(),
                any(), any(), any(), any());
    }

    @Test
    void doFilter_blankApiKeyHeader_passesThroughWithoutCallingAuthService() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("X-API-KEY", "   ");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL)).isNull();
        verify(apiKeyAuthService, never()).validate(any(), any(), any(), any(), anyLong(),
                any(), any(), any(), any());
    }

    // ---- Valid API key ----

    @Test
    void doFilter_validApiKey_setsPrincipalOnRequest() throws Exception {
        ApiKeyAuthResult success = ApiKeyAuthResult.builder()
                .valid(true)
                .apiKeyId(CRED_ID)
                .code("code123456789")
                .name("My Key")
                .permissions(Set.of("read:base:file"))
                .signatureVerified(false)
                .build();
        when(apiKeyAuthService.validate(eq(VALID_API_KEY), anyString(), isNull(), isNull(),
                anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(success);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        ApiKeyPrincipal principal = (ApiKeyPrincipal) chain.getRequest()
                .getAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL);
        assertThat(principal).isNotNull();
        assertThat(principal.getApiKeyId()).isEqualTo(CRED_ID);
        assertThat(principal.getCode()).isEqualTo("code123456789");
        assertThat(principal.getPermissions()).containsExactly("read:base:file");
        assertThat(principal.isSignatureVerified()).isFalse();
    }

    @Test
    void doFilter_validApiKeyWithSignature_principalHasSignatureVerifiedTrue() throws Exception {
        ApiKeyAuthResult success = ApiKeyAuthResult.builder()
                .valid(true).apiKeyId(CRED_ID).code("code123456789")
                .name("Signed Key").permissions(Set.of("write:base:file"))
                .signatureVerified(true).build();
        when(apiKeyAuthService.validate(anyString(), anyString(), eq("sig-abc"), anyLong(),
                anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(success);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/files");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        request.addHeader("X-Signature", "sig-abc");
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        ApiKeyPrincipal principal = (ApiKeyPrincipal) chain.getRequest()
                .getAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL);
        assertThat(principal).isNotNull();
        assertThat(principal.isSignatureVerified()).isTrue();
    }

    // ---- Invalid API key ----

    @Test
    void doFilter_invalidApiKey_logWarningAndPassThroughWithoutPrincipal() throws Exception {
        when(apiKeyAuthService.validate(anyString(), anyString(), any(), any(), anyLong(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ApiKeyAuthResult.failure("API_KEY_INVALID", "Invalid key"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files");
        request.addHeader("X-API-KEY", "bad.key");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(chain.getRequest()
                .getAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL)).isNull();
    }

    // ---- Body caching ----

    @Test
    void doFilter_validApiKey_downstreamReceivesCachedBody() throws Exception {
        String body = "{\"key\":\"value\"}";
        ApiKeyAuthResult success = ApiKeyAuthResult.builder()
                .valid(true).apiKeyId(CRED_ID).code("code").name("k")
                .permissions(Set.of()).signatureVerified(false).build();
        when(apiKeyAuthService.validate(anyString(), anyString(), any(), any(), anyLong(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(success);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // Downstream receives a CachedBodyHttpServletRequest, can re-read the body
        byte[] downstream = chain.getRequest().getInputStream().readAllBytes();
        assertThat(new String(downstream, StandardCharsets.UTF_8)).isEqualTo(body);
    }

    // ---- IP resolution ----

    @Test
    void doFilter_xForwardedForHeader_usesFirstIpAsClientIp() throws Exception {
        when(apiKeyAuthService.validate(anyString(), eq("10.0.0.1"), any(), any(), anyLong(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ApiKeyAuthResult.failure("API_KEY_INVALID", "test"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        request.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.1");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // Verify validate was called with the first IP from X-Forwarded-For
        verify(apiKeyAuthService).validate(anyString(), eq("10.0.0.1"), any(), any(), anyLong(),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void doFilter_noXForwardedFor_usesRemoteAddr() throws Exception {
        when(apiKeyAuthService.validate(anyString(), eq("192.168.1.50"), any(), any(), anyLong(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ApiKeyAuthResult.failure("API_KEY_INVALID", "test"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        request.setRemoteAddr("192.168.1.50");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(apiKeyAuthService).validate(anyString(), eq("192.168.1.50"), any(), any(), anyLong(),
                anyString(), anyString(), anyString(), anyString());
    }

    // ---- Query string sorting ----

    @Test
    void doFilter_queryParams_areSortedAlphabeticallyForSignature() throws Exception {
        // Validate is called with sorted query string "a=1&b=2&c=3" regardless of original order
        when(apiKeyAuthService.validate(anyString(), anyString(), any(), any(), anyLong(),
                anyString(), anyString(), eq("a=1&b=2&c=3"), anyString()))
                .thenReturn(ApiKeyAuthResult.failure("API_KEY_INVALID", "test"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/search");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        // setQueryString makes getQueryString() non-null (triggers param-map iteration in filter)
        request.setQueryString("c=3&a=1&b=2");
        request.addParameter("a", "1");
        request.addParameter("b", "2");
        request.addParameter("c", "3");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(apiKeyAuthService).validate(anyString(), anyString(), any(), any(), anyLong(),
                anyString(), anyString(), eq("a=1&b=2&c=3"), anyString());
    }
}
