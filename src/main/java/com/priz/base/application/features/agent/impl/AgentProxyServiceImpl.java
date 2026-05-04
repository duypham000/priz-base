package com.priz.base.application.features.agent.impl;

import com.priz.base.application.features.agent.AgentProxyService;
import com.priz.base.application.features.agent.converter.AgentProxyConverter;
import com.priz.base.application.features.agent.dto.ModelDto;
import com.priz.base.application.features.agent.dto.ProviderDto;
import com.priz.base.application.features.agent.dto.SessionDto;
import com.priz.base.application.features.agent.dto.WindowUsageDto;
import com.priz.base.infrastructure.grpc.client.AgentQueryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/*
@Service
@RequiredArgsConstructor
*/
public class AgentProxyServiceImpl implements AgentProxyService {

    private final AgentQueryClient client = null;

    @Override
    public List<ModelDto> listModels() {
        return client.listModels().stream().map(AgentProxyConverter::toDto).toList();
    }

    @Override
    public List<ProviderDto> listProviders() {
        return client.listProviders().stream().map(AgentProxyConverter::toDto).toList();
    }

    @Override
    public List<SessionDto> listSessions(String userId, boolean onlyActive) {
        return client.listSessions(userId, onlyActive).stream().map(AgentProxyConverter::toDto).toList();
    }

    @Override
    public List<WindowUsageDto> getTokenUsage(String subjectType, String subjectId) {
        return client.getTokenUsage(subjectType, subjectId).stream().map(AgentProxyConverter::toDto).toList();
    }
}
