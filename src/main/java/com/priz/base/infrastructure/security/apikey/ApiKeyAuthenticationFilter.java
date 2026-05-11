package com.priz.base.infrastructure.security.apikey;

import com.priz.base.application.features.apikey.ApiKeyAuthService;
import com.priz.base.application.features.apikey.dto.ApiKeyAuthResult;
import com.priz.common.security.ApiKeyPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(1)
@ConditionalOnBean(ApiKeyAuthService.class)
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_API_KEY_PRINCIPAL = "apiKeyPrincipal";

    private static final String HEADER_API_KEY = "X-API-KEY";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final long DEFAULT_RECV_WINDOW_MS = 5000L;

    private final ApiKeyAuthService apiKeyAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String body = cachedRequest.getCachedBodyAsString();
        String signature = request.getHeader(HEADER_SIGNATURE);
        Long timestamp = parseTimestamp(request.getHeader(HEADER_TIMESTAMP));
        String clientIp = resolveClientIp(request);
        String queryStr = sortedQueryString(request);

        ApiKeyAuthResult result = apiKeyAuthService.validate(
                apiKey, clientIp, signature, timestamp, DEFAULT_RECV_WINDOW_MS,
                request.getMethod(), request.getRequestURI(), queryStr, body);

        if (!result.isValid()) {
            log.warn("API key auth failed: {} - {}", result.getErrorCode(), result.getErrorMessage());
            chain.doFilter(cachedRequest, response);
            return;
        }

        ApiKeyPrincipal principal = ApiKeyPrincipal.builder()
                .apiKeyId(result.getApiKeyId())
                .code(result.getCode())
                .name(result.getName())
                .permissions(result.getPermissions())
                .signatureVerified(result.isSignatureVerified())
                .build();

        cachedRequest.setAttribute(ATTR_API_KEY_PRINCIPAL, principal);
        chain.doFilter(cachedRequest, response);
    }

    private Long parseTimestamp(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sortedQueryString(HttpServletRequest request) {
        if (request.getQueryString() == null) return "";
        return request.getParameterMap().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining("&"));
    }
}
