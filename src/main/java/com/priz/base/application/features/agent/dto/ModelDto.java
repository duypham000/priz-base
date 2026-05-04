package com.priz.base.application.features.agent.dto;

import lombok.Builder;

@Builder
public record ModelDto(
        String modelId,
        String provider,
        int maxTokens,
        boolean supportsThinking,
        boolean supportsTools
) {
}
