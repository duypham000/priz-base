package com.priz.base.infrastructure.grpc.client;

import com.priz.common.grpc.AbstractGrpcClient;
import com.priz.interfaces.agent.query.AgentQueryServiceGrpc;
import com.priz.interfaces.agent.query.GetTokenUsageRequest;
import com.priz.interfaces.agent.query.ListModelsRequest;
import com.priz.interfaces.agent.query.ListProvidersRequest;
import com.priz.interfaces.agent.query.ListSessionsRequest;
import com.priz.interfaces.agent.query.ModelInfo;
import com.priz.interfaces.agent.query.PingRequest;
import com.priz.interfaces.agent.query.ProviderInfo;
import com.priz.interfaces.agent.query.SessionSummary;
import com.priz.interfaces.agent.query.WindowUsage;

import java.time.Duration;
import java.util.List;

/**
 * Blocking client for the agent service's {@code AgentQueryService}.
 * <p>
 * Every call passes through {@link AbstractGrpcClient#call} which applies the default deadline and
 * translates {@code StatusRuntimeException} via {@link com.priz.common.grpc.GrpcErrorMapper}.
 * Callers receive {@link com.priz.common.exception.GrpcUnavailableException} when the peer is
 * unreachable — the global exception handler maps that to HTTP 503.
 */
public class AgentQueryClient extends AbstractGrpcClient {

    static final String PEER_NAME = "agent-service";
    private static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(5);

    private final AgentQueryServiceGrpc.AgentQueryServiceBlockingStub stub;

    public AgentQueryClient(AgentQueryServiceGrpc.AgentQueryServiceBlockingStub stub) {
        super(PEER_NAME, DEFAULT_DEADLINE);
        this.stub = stub;
    }

    public long ping() {
        return call(stub, s -> s.ping(PingRequest.getDefaultInstance())).getServerTimeEpochMs();
    }

    public List<ModelInfo> listModels() {
        return call(stub, s -> s.listModels(ListModelsRequest.getDefaultInstance())).getModelsList();
    }

    public List<ProviderInfo> listProviders() {
        return call(stub, s -> s.listProviders(ListProvidersRequest.getDefaultInstance())).getProvidersList();
    }

    public List<SessionSummary> listSessions(String userId, boolean onlyActive) {
        ListSessionsRequest request = ListSessionsRequest.newBuilder()
                .setUserId(userId)
                .setOnlyActive(onlyActive)
                .build();
        return call(stub, s -> s.listSessions(request)).getSessionsList();
    }

    public List<WindowUsage> getTokenUsage(String subjectType, String subjectId) {
        GetTokenUsageRequest request = GetTokenUsageRequest.newBuilder()
                .setSubjectType(subjectType)
                .setSubjectId(subjectId)
                .build();
        return call(stub, s -> s.getTokenUsage(request)).getWindowsList();
    }
}
