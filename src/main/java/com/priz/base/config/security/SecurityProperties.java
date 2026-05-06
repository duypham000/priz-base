package com.priz.base.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties("base.security")
public record SecurityProperties(
        @DefaultValue({
                "/actuator/**",
                "/api/auth/**",
                "/api/health",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/mcp/**",
                "/api/test/**"
        }) List<String> publicPaths
) {}
