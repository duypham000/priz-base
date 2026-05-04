package com.priz.base.infrastructure.grpc.client;

import com.priz.common.grpc.AbstractGrpcHealthProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Startup probe for the agent service. Runs on {@code ApplicationReadyEvent} — failure is logged
 * as WARN and never blocks boot.
 */
@Component
@RequiredArgsConstructor
public class AgentGrpcHealthProbe extends AbstractGrpcHealthProbe {

    private final AgentQueryClient client;

    @Override
    protected String peerName() {
        return AgentQueryClient.PEER_NAME;
    }

    @Override
    protected void ping() {
        client.ping();
    }
}
