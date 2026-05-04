package com.priz.base.application.features.agent.converter;

import com.priz.base.application.features.agent.dto.ModelDto;
import com.priz.base.application.features.agent.dto.ProviderDto;
import com.priz.base.application.features.agent.dto.SessionDto;
import com.priz.base.application.features.agent.dto.WindowUsageDto;
import com.priz.interfaces.agent.query.ModelInfo;
import com.priz.interfaces.agent.query.ProviderInfo;
import com.priz.interfaces.agent.query.SessionSummary;
import com.priz.interfaces.agent.query.WindowUsage;

public final class AgentProxyConverter {

    private AgentProxyConverter() {
    }

    public static ModelDto toDto(ModelInfo proto) {
        return ModelDto.builder()
                .modelId(proto.getModelId())
                .provider(proto.getProvider())
                .maxTokens(proto.getMaxTokens())
                .supportsThinking(proto.getSupportsThinking())
                .supportsTools(proto.getSupportsTools())
                .build();
    }

    public static ProviderDto toDto(ProviderInfo proto) {
        return ProviderDto.builder()
                .providerName(proto.getProviderName())
                .baseUrl(proto.getBaseUrl())
                .enabled(proto.getEnabled())
                .rateLimitRpm(proto.getRateLimitRpm())
                .build();
    }

    public static SessionDto toDto(SessionSummary proto) {
        return SessionDto.builder()
                .sessionId(proto.getSessionId())
                .agentId(proto.getAgentId())
                .userId(proto.getUserId())
                .state(proto.getState())
                .activeModelId(proto.getActiveModelId())
                .metadata(proto.getMetadataMap())
                .build();
    }

    public static WindowUsageDto toDto(WindowUsage proto) {
        return WindowUsageDto.builder()
                .window(proto.getWindow())
                .used(proto.getUsed())
                .limit(proto.getLimit())
                .remaining(proto.getRemaining())
                .windowStartEpochMs(proto.getWindowStartEpochMs())
                .windowResetEpochMs(proto.getWindowResetEpochMs())
                .build();
    }
}
