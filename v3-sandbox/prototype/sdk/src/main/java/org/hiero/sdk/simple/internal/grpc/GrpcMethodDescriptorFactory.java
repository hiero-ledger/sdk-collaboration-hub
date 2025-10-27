package org.hiero.sdk.simple.internal.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

import com.google.protobuf.MessageLite;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class GrpcMethodDescriptorFactory {

    public static final Map<String, MethodDescriptor<?, ?>> METHOD_DESCRIPTORS = new ConcurrentHashMap<>();

    private static final Lock LOCK = new java.util.concurrent.locks.ReentrantLock();

    private static <ReqT, RespT> Optional<MethodDescriptor<ReqT, RespT>> fromCache(String identifier) {
        if (METHOD_DESCRIPTORS.containsKey(identifier)) {
            final MethodDescriptor<ReqT, RespT> cachedValue = (MethodDescriptor<ReqT, RespT>) METHOD_DESCRIPTORS.get(
                    identifier);
            return Optional.ofNullable(cachedValue);
        }
        return Optional.empty();
    }

    public static <Req extends MessageLite, Resp extends MessageLite> MethodDescriptor<Req, Resp> getOrCreateMethodDescriptor(
            String serviceName,
            String methodName, Supplier<Req> defaultRequestSupplier, Supplier<Resp> defaultResponseSupplier) {
        final String identifier = serviceName + "." + methodName;
        final Optional<MethodDescriptor<Req, Resp>> fromCache = fromCache(identifier);
        if (fromCache.isPresent()) {
            return fromCache.get();
        }
        LOCK.lock();
        try {
            final Optional<MethodDescriptor<Req, Resp>> fromCacheCheck2 = fromCache(identifier);
            if (fromCacheCheck2.isPresent()) {
                return fromCacheCheck2.get();
            }
            final MethodDescriptor<Req, Resp> methodDescriptor =
                    MethodDescriptor.<Req, Resp>newBuilder()
                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                            .setFullMethodName(generateFullMethodName(serviceName, methodName))
                            .setSampledToLocalTracing(true)
                            .setRequestMarshaller(ProtoLiteUtils.marshaller(defaultRequestSupplier.get()))
                            .setResponseMarshaller(ProtoLiteUtils.marshaller(defaultResponseSupplier.get()))
                            .build();
            METHOD_DESCRIPTORS.put(identifier, methodDescriptor);
            return methodDescriptor;
        } finally {
            LOCK.unlock();
        }
    }
}
