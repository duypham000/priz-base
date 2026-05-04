package com.priz.base.infrastructure.grpc.client;

import com.priz.interfaces.agent.query.AgentQueryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Spring bean definitions for outbound gRPC stubs and client wrappers.
 * <p>
 * Channels are created lazily by {@link GrpcChannelFactory} — if the peer is unreachable at
 * startup the beans still initialize and failures are surfaced only on the first actual RPC.
 * {@link AgentGrpcHealthProbe} logs a WARN on {@code ApplicationReadyEvent} but never blocks boot.
 */
@Configuration
public class GrpcClientConfig {

    @Bean
    AgentQueryServiceGrpc.AgentQueryServiceBlockingStub agentQueryStub(GrpcChannelFactory channelFactory) {
        return AgentQueryServiceGrpc.newBlockingStub(channelFactory.createChannel("agent-service"));
    }

    @Bean
    AgentQueryClient agentQueryClient(AgentQueryServiceGrpc.AgentQueryServiceBlockingStub stub) {
        return new AgentQueryClient(stub);
    }
}
