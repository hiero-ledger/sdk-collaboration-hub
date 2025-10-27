package org.hiero.sdk.simple.internal.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.hiero.sdk.simple.network.ConsensusNode;
import org.jspecify.annotations.NonNull;

public final class GrpcChannelFactory {

    public static Channel createChannel(@NonNull final ConsensusNode node, @NonNull Executor executor) {
        Objects.requireNonNull(node, "node must not be null");
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(node.getAddress())
                .usePlaintext();
        return channelBuilder.keepAliveTimeout(10L, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .disableRetry()
                .executor(executor)
                .build();
    }
}
