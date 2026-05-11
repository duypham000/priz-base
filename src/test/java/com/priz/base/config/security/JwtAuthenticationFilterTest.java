package com.priz.base.config.security;

import com.priz.base.infrastructure.security.apikey.ApiKeyAuthenticationFilter;
import com.priz.common.security.ApiKeyPrincipal;
import com.priz.common.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @Test
    void doFilter_OPTIONS_passesThroughWithoutParsingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/test");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isNull();
        verify(jwtService, never()).parseToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_noAuthorizationHeader_passesThroughWithoutSettingAttrs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_ROLES)).isNull();
    }

    @Test
    void doFilter_authHeaderWithoutBearerPrefix_passesThroughWithoutParsing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isNull();
        verify(jwtService, never()).parseToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_validToken_setsUserIdAndRoles() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-123");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(claims.get("perms", String.class)).thenReturn(null);
        when(jwtService.parseToken("valid.jwt.token")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isEqualTo("user-123");
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_ROLES))
                .isEqualTo(List.of("ADMIN"));
    }

    @Test
    void doFilter_validTokenWithNoRole_setsEmptyRoles() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-456");
        when(claims.get("role", String.class)).thenReturn(null);
        when(claims.get("perms", String.class)).thenReturn(null);
        when(jwtService.parseToken("valid.jwt.norole")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profile");
        request.addHeader("Authorization", "Bearer valid.jwt.norole");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isEqualTo("user-456");
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_ROLES)).isEqualTo(List.of());
    }

    @Test
    void doFilter_expiredToken_passesThroughWithoutSettingAttrs() throws Exception {
        when(jwtService.parseToken("expired.jwt.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isNull();
    }

    @Test
    void doFilter_malformedToken_passesThroughWithoutSettingAttrs() throws Exception {
        when(jwtService.parseToken("bad.token"))
                .thenThrow(new MalformedJwtException("Malformed"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("Authorization", "Bearer bad.token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isNull();
    }

    @Test
    void doFilter_apiKeyPrincipalAlreadySet_skipsJwtProcessing() throws Exception {
        ApiKeyPrincipal principal = ApiKeyPrincipal.builder()
                .apiKeyId("key-1").code("code").build();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/files");
        request.addHeader("Authorization", "Bearer some.jwt.token");
        request.setAttribute(ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL, principal);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // JWT was NOT parsed — API key takes priority
        verify(jwtService, never()).parseToken(org.mockito.ArgumentMatchers.anyString());
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isNull();
    }

    // ---- permsMap tests ----

    @Test
    @SuppressWarnings("unchecked")
    void doFilter_tokenWithPermsClaim_setsPermsMapFromClaim() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("perms", String.class)).thenReturn("base:article=5,global:report=2");
        when(jwtService.parseToken("perms.jwt.token")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("Authorization", "Bearer perms.jwt.token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Map<String, Integer> permsMap =
                (Map<String, Integer>) request.getAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP);
        assertThat(permsMap)
                .containsEntry("base:article", 5)
                .containsEntry("global:report", 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void doFilter_xUserPermissionsHeaderPresent_headerTakesPriorityOverClaim() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("role", String.class)).thenReturn("USER");
        // JWT claim has stale perms
        when(jwtService.parseToken("perms.jwt.token")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("Authorization", "Bearer perms.jwt.token");
        // Kong injected header with updated perms
        request.addHeader("X-User-Permissions", "base:article=7");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Map<String, Integer> permsMap =
                (Map<String, Integer>) request.getAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP);
        assertThat(permsMap).containsEntry("base:article", 7);
        // claim was NOT consulted for perms since header was present
        verify(claims, never()).get("perms", String.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void doFilter_noPermsClaimOrHeader_setsEmptyPermsMap() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("perms", String.class)).thenReturn(null);
        when(jwtService.parseToken("noperms.jwt.token")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("Authorization", "Bearer noperms.jwt.token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Map<String, Integer> permsMap =
                (Map<String, Integer>) request.getAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP);
        assertThat(permsMap).isEmpty();
    }
}
