package com.priz.base.config.database;

import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            SecurityContext ctx = SecurityContextHolder.get();
            if (ctx != null && ctx.getUserId() != null) {
                return Optional.of(ctx.getUserId());
            }
            return Optional.of("system");
        };
    }
}
