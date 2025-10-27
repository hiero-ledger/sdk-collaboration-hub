package org.hiero.sdk.simple.internal;

import com.hedera.hashgraph.sdk.proto.SignatureMap;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.PackedTransaction;
import org.hiero.sdk.simple.Response;
import org.hiero.sdk.simple.grpc.GrpcClient;
import org.hiero.sdk.simple.internal.util.ProtobufUtil;
import org.hiero.sdk.simple.network.TransactionId;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.hiero.sdk.simple.transactions.spi.TransactionProtobuffSupport;
import org.jspecify.annotations.NonNull;

public final class DefaultPackedTransaction<R extends Response, T extends org.hiero.sdk.simple.Transaction<T, R>> implements
        PackedTransaction<T, R> {

    private final Map<PublicKey, byte[]> transactionSignatures = new HashMap<>();

    private final TransactionBody transactionBody;

    private final HieroClient client;

    private final TransactionProtobuffSupport<R, T> transactionFactory;

    public DefaultPackedTransaction(
            @NonNull final TransactionBody transactionBody,
            @NonNull final TransactionProtobuffSupport<R, T> transactionFactory,
            @NonNull final HieroClient client) {
        this.transactionBody = Objects.requireNonNull(transactionBody, "transactionBody must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.transactionFactory = Objects.requireNonNull(transactionFactory, "transactionFactory must not be null");
        if (client.signTransactionsAutomaticallyWithOperator()) {
            sign(client.getOperatorAccount().keyPair());
        }
    }

    @Override
    public TransactionId transactionId() {
        return ProtobufUtil.fromProtobuf(transactionBody.getTransactionID());
    }

    @Override
    public @NonNull PackedTransaction sign(@NonNull final PublicKey publicKey,
            @NonNull final UnaryOperator<byte[]> transactionSigner) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(transactionSigner, "transactionSigner must not be null");

        if (transactionSignatures.containsKey(publicKey)) {
            throw new IllegalStateException("transaction is already signed with public key '" + publicKey + "'");
        }
        final byte[] transactionBytes = transactionBody.toByteArray();
        final byte[] signature = transactionSigner.apply(transactionBytes);
        transactionSignatures.put(publicKey, signature);
        return this;
    }

    @Override
    public CompletableFuture<R> send() {
        Objects.requireNonNull(client, "client must not be null");
        final Transaction protobufTransaction = createProtobufTransaction();
        final GrpcClient grpcClient = client.getGrpcClient();
        final MethodDescriptor<Transaction, TransactionResponse> methodDescriptor = transactionFactory.getMethodDescriptor();
        return grpcClient.call(methodDescriptor, protobufTransaction).handle((response, throwable) -> {
            if (throwable != null) {
                throw new RuntimeException("Transaction execution failed", throwable);
            }
            return transactionFactory.createResponse(client, protobufTransaction, response);
        });
    }

    @Override
    public R sendAndWait() throws ExecutionException, InterruptedException, TimeoutException {
        final long timeout = client.getDefaultTimeoutInMs();
        return send().get(timeout, TimeUnit.MILLISECONDS);
    }

    private Transaction createProtobufTransaction() {
        final SignatureMap.Builder signatureBuilder = SignatureMap.newBuilder();
        transactionSignatures.entrySet().forEach(entry -> {
            signatureBuilder.addSigPair(ProtobufUtil.toSignaturePairProtobuf(entry.getKey(), entry.getValue()));
        });
        return Transaction.newBuilder()
                .setBodyBytes(transactionBody.toByteString())
                .setSigMap(signatureBuilder.build())
                .build();
    }

    @Override
    public T unpack() {
        return transactionFactory.unpack(transactionBody);
    }
}
