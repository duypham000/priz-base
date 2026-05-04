package com.priz.base.application.features.agent.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record SessionDto(
        String sessionId,
        String agentId,
        String userId,
        String state,
        String activeModelId,
        Map<String, String> metadata
) {
}
