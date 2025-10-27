package org.hiero.sdk.simple.internal.grpc;

import com.google.protobuf.MessageLite;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientCall.Listener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.hiero.sdk.simple.grpc.GrpcClient;
import org.hiero.sdk.simple.network.ConsensusNode;
import org.jspecify.annotations.NonNull;

public final class GrpcClientImpl implements GrpcClient {

    private final Channel channel;

    public GrpcClientImpl(@NonNull final ConsensusNode node, @NonNull final Executor executor) {
        this.channel = GrpcChannelFactory.createChannel(node, executor);
    }

    @Override
    public <I extends MessageLite, O extends MessageLite> CompletableFuture<O> call(
            MethodDescriptor<I, O> methodDescriptor, I input) {
        Objects.requireNonNull(methodDescriptor, "methodDescriptor must not be null");
        final CompletableFuture<O> future = new CompletableFuture<>();
        final ClientCall<I, O> call = channel.newCall(methodDescriptor,
                CallOptions.DEFAULT);
        call.start(new Listener<>() {

            @Override
            public void onMessage(O response) {
                future.complete(response);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (!future.isDone()) {
                    future.completeExceptionally(
                            new RuntimeException("Call failed with status: " + status, status.asException()));
                }
            }
        }, new Metadata());
        call.sendMessage(input);
        call.halfClose();
        call.request(1);
        return future;
    }
}
