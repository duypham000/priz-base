package com.priz.base.interfaces.grpc;

import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.interfaces.user.GetUserContextRequest;
import com.priz.interfaces.user.GetUserRequest;
import com.priz.interfaces.user.ListActiveUsersRequest;
import com.priz.interfaces.user.ListActiveUsersResponse;
import com.priz.interfaces.user.ResourceQuotas;
import com.priz.interfaces.user.UserExecutionContext;
import com.priz.interfaces.user.UserInfo;
import com.priz.interfaces.user.UserServiceGrpc;
import com.priz.interfaces.user.UserSummary;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    @Value("${grpc.user-service.default-quotas.max-sessions:5}")
    private int defaultMaxSessions;

    @Value("${grpc.user-service.default-quotas.requests-per-minute:60}")
    private int defaultRequestsPerMinute;

    @Value("${grpc.user-service.default-quotas.max-tokens-per-day:100000}")
    private int defaultMaxTokensPerDay;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserInfo> responseObserver) {
        userRepository.findById(request.getUserId())
                .ifPresentOrElse(
                        user -> responseObserver.onNext(toUserInfo(user)),
                        () -> responseObserver.onError(Status.NOT_FOUND
                                .withDescription("User not found")
                                .asRuntimeException())
                );
        responseObserver.onCompleted();
    }

    @Override
    public void getUserContext(GetUserContextRequest request, StreamObserver<UserExecutionContext> responseObserver) {
        userRepository.findById(request.getUserId())
                .ifPresentOrElse(
                        user -> responseObserver.onNext(UserExecutionContext.newBuilder()
                                .setUserId(user.getId())
                                .setQuotas(ResourceQuotas.newBuilder()
                                        .setMaxSessions(defaultMaxSessions)
                                        .setRequestsPerMinute(defaultRequestsPerMinute)
                                        .setMaxTokensPerDay(defaultMaxTokensPerDay)
                                        .build())
                                .build()),
                        () -> responseObserver.onError(Status.NOT_FOUND
                                .withDescription("User not found")
                                .asRuntimeException())
                );
        responseObserver.onCompleted();
    }

    @Override
    public void listActiveUsers(ListActiveUsersRequest request, StreamObserver<ListActiveUsersResponse> responseObserver) {
        List<UserSummary> summaries = userRepository.findAll().stream()
                .limit(10)
                .map(user -> UserSummary.newBuilder()
                        .setUserId(user.getId())
                        .build())
                .toList();

        responseObserver.onNext(ListActiveUsersResponse.newBuilder()
                .addAllUsers(summaries)
                .build());
        responseObserver.onCompleted();
    }

    private UserInfo toUserInfo(UserModel user) {
        return UserInfo.newBuilder()
                .setUserId(user.getId())
                .setEmail(user.getEmail())
                .addRoles(user.getRole().name())
                .build();
    }
}
