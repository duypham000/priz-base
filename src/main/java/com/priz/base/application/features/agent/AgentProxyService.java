package com.priz.base.application.features.agent;

import com.priz.base.application.features.agent.dto.ModelDto;
import com.priz.base.application.features.agent.dto.ProviderDto;
import com.priz.base.application.features.agent.dto.SessionDto;
import com.priz.base.application.features.agent.dto.WindowUsageDto;

import java.util.List;

/**
 * Read-only proxy over the agent service. Every method proxies a single gRPC call; when the peer
 * is unreachable the underlying client raises {@code GrpcUnavailableException} (HTTP 503).
 */
public interface AgentProxyService {

    List<ModelDto> listModels();

    List<ProviderDto> listProviders();

    List<SessionDto> listSessions(String userId, boolean onlyActive);

    List<WindowUsageDto> getTokenUsage(String subjectType, String subjectId);
}
