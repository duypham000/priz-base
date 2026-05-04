package com.priz.base.application.features.agent.dto;

import lombok.Builder;

@Builder
public record WindowUsageDto(
        String window,
        long used,
        long limit,
        long remaining,
        long windowStartEpochMs,
        long windowResetEpochMs
) {
}
