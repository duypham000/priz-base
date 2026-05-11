package com.priz.base.testutil;

import com.priz.base.config.security.SecurityProperties;
import com.priz.common.security.jwt.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class WebMvcSecurityConfig {

    @Bean
    @Primary
    public JwtService jwtService() {
        return mock(JwtService.class);
    }

    @Bean
    @Primary
    public SecurityProperties securityProperties() {
        return new SecurityProperties(List.of());
    }
}
