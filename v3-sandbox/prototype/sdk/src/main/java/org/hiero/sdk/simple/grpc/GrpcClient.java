package org.hiero.sdk.simple.grpc;

import com.google.protobuf.MessageLite;
import io.grpc.MethodDescriptor;
import java.util.concurrent.CompletableFuture;

/**
 * A simple gRPC client interface for making calls to a gRPC service.
 */
public interface GrpcClient {

    /**
     * Calls a gRPC method with the specified input message and returns a {@link CompletableFuture} that will complete
     * with the output message.
     *
     * @param <I>              the type of the input message
     * @param <O>              the type of the output message
     * @param methodDescriptor the MethodDescriptor for the gRPC method to call
     * @param input            the input message to send
     * @return a CompletableFuture that will complete with the output message
     */
    <I extends MessageLite, O extends MessageLite> CompletableFuture<O> call(
            MethodDescriptor<I, O> methodDescriptor, I input);

}
