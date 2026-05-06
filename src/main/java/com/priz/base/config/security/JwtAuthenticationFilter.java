package com.priz.base.config.security;

import com.priz.common.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLES = "roles";

    private static final String BEARER_PREFIX = "Bearer ";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Missing or malformed Authorization header for path: {}", path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parseToken(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            List<String> roles = role != null ? List.of(role) : List.of();

            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_ROLES, roles);

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token for path {}: {}", path, e.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token for path {}: {}", path, e.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    private boolean isPublicPath(String path) {
        return securityProperties.publicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
