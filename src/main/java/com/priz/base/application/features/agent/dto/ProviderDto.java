package com.priz.base.application.features.agent.dto;

import lombok.Builder;

@Builder
public record ProviderDto(
        String providerName,
        String baseUrl,
        boolean enabled,
        int rateLimitRpm
) {
}
