package com.priz.base.config.security;

import com.priz.common.security.jwt.JwtService;
import com.priz.common.security.permission.PermissionCodec;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLES = "roles";
    public static final String ATTR_PERMS_MAP = "permsMap";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_X_USER_PERMISSIONS = "X-User-Permissions";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (request.getAttribute(
                com.priz.base.infrastructure.security.apikey.ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL)
                != null) {
            chain.doFilter(request, response);
            return;
        }

        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.isBlank()) {
            log.info("Trusting header X-User-Id: {}", headerUserId);
            request.setAttribute(ATTR_USER_ID, headerUserId);
            request.setAttribute(ATTR_ROLES, List.of("USER"));
            
            String permsEncoded = request.getHeader(HEADER_X_USER_PERMISSIONS);
            log.info("X-User-Permissions header: {}", permsEncoded);
            if (permsEncoded != null && !permsEncoded.isBlank()) {
                Map<String, Integer> permsMap = PermissionCodec.decode(permsEncoded);
                log.info("Decoded permsMap: {}", permsMap);
                request.setAttribute(ATTR_PERMS_MAP, permsMap);
            }
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        String path = request.getRequestURI();
        try {
            Claims claims = jwtService.parseToken(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            List<String> roles = role != null ? List.of(role) : List.of();

            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_ROLES, roles);

            // X-User-Permissions header (injected by Kong) takes priority over JWT claim
            String permsEncoded = request.getHeader(HEADER_X_USER_PERMISSIONS);
            if (permsEncoded == null || permsEncoded.isBlank()) {
                permsEncoded = claims.get("perms", String.class);
            }
            Map<String, Integer> permsMap = PermissionCodec.decode(permsEncoded);
            request.setAttribute(ATTR_PERMS_MAP, permsMap);

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token for path {}: {}", path, e.getMessage());
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token for path {}: {}", path, e.getMessage());
            chain.doFilter(request, response);
        }
    }
}
