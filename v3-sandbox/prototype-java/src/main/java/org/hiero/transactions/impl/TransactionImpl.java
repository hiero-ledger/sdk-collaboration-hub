package org.hiero.transactions.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import org.hiero.client.HieroClient;
import org.hiero.common.AccountId;
import org.hiero.keys.KeyPair;
import org.hiero.keys.PublicKey;
import org.hiero.transactions.Receipt;
import org.hiero.transactions.Record;
import org.hiero.transactions.Response;
import org.hiero.transactions.Transaction;
import org.hiero.transactions.TransactionBuilder;
import org.hiero.transactions.TransactionSigner;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Concrete final {@link Transaction} implementation. The {@code @@finalType}
 * annotation in the meta-language is enforced here by making the class
 * {@code final}.
 *
 * <p>The prototype intentionally does not implement protobuf-based serialisation
 * or actual gRPC submission — that lives behind the SPI which is itself only a
 * placeholder in the meta-language. {@link #toBytes()} returns an empty array,
 * {@link #execute(HieroClient)} returns a failed future, and {@link #fromBytes(byte[])}
 * always throws.
 */
public final class TransactionImpl implements Transaction {

    private final TransactionBody body;

    @Nullable
    private volatile Integer maxAttempts;

    @Nullable
    private volatile Long maxBackoff;

    @Nullable
    private volatile Long minBackoff;

    @Nullable
    private volatile Long attemptTimeout;

    /**
     * Per-node signature map. Each entry is keyed by the node account id and
     * contains a sub-map keyed by the public key that produced the signature.
     */
    private final ConcurrentHashMap<AccountId, ConcurrentHashMap<PublicKey, byte[]>> signatures =
            new ConcurrentHashMap<>();

    public TransactionImpl(@NonNull final TransactionBody body) {
        this.body = Objects.requireNonNull(body, "body must not be null");
    }

    @NonNull
    public TransactionBody body() {
        return body;
    }

    @Override
    @Nullable
    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public void setMaxAttempts(@Nullable final Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    @Nullable
    public Long getMaxBackoff() {
        return maxBackoff;
    }

    @Override
    public void setMaxBackoff(@Nullable final Long maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    @Override
    @Nullable
    public Long getMinBackoff() {
        return minBackoff;
    }

    @Override
    public void setMinBackoff(@Nullable final Long minBackoff) {
        this.minBackoff = minBackoff;
    }

    @Override
    @Nullable
    public Long getAttemptTimeout() {
        return attemptTimeout;
    }

    @Override
    public void setAttemptTimeout(@Nullable final Long attemptTimeout) {
        this.attemptTimeout = attemptTimeout;
    }

    @Override
    @NonNull
    public Transaction sign(@NonNull final KeyPair keyPair) {
        Objects.requireNonNull(keyPair, "keyPair must not be null");
        final byte[] bytes = toBytes();
        final byte[] signature = keyPair.privateKey().sign(bytes);
        recordSignaturePerNode(keyPair.publicKey(), signature);
        return this;
    }

    @Override
    @NonNull
    public Transaction sign(@NonNull final PublicKey publicKey, @NonNull final TransactionSigner transactionSigner) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(transactionSigner, "transactionSigner must not be null");
        final byte[] bytes = toBytes();
        final byte[] signature = transactionSigner.signTransaction(bytes);
        recordSignaturePerNode(publicKey, signature);
        return this;
    }

    private void recordSignaturePerNode(@NonNull final PublicKey publicKey, @NonNull final byte[] signature) {
        for (final AccountId nodeAccountId : body.nodeAccountIds()) {
            signatures.computeIfAbsent(nodeAccountId, id -> new ConcurrentHashMap<>())
                    .put(publicKey, signature.clone());
        }
    }

    @Override
    @NonNull
    public Map<AccountId, Map<PublicKey, byte[]>> getSignatures() {
        // Two-level immutable view: copyOf each inner map and the outer map.
        final ConcurrentHashMap<AccountId, Map<PublicKey, byte[]>> snapshot = new ConcurrentHashMap<>();
        signatures.forEach((nodeId, perKey) -> snapshot.put(nodeId, Map.copyOf(perKey)));
        return Collections.unmodifiableMap(snapshot);
    }

    @Override
    @NonNull
    public CompletionStage<Response<? extends Receipt, ? extends Record<? extends Receipt>>> execute(
            @NonNull final HieroClient client) {
        Objects.requireNonNull(client, "client must not be null");
        // The prototype does not implement actual transmission — see REPORT.md.
        final CompletableFuture<Response<? extends Receipt, ? extends Record<? extends Receipt>>> future =
                new CompletableFuture<>();
        future.completeExceptionally(
                new UnsupportedOperationException("Transaction execution is not implemented in the prototype"));
        return future;
    }

    @Override
    @NonNull
    public byte[] toBytes() {
        // The prototype does not implement protobuf serialisation — see REPORT.md.
        // Returning an empty array keeps the API usable for non-network tests.
        return new byte[0];
    }

    @Override
    @NonNull
    public TransactionBuilder<?, ?, ?, ?> unbuild() {
        throw new UnsupportedOperationException(
                "unbuild() requires SPI lookup of the original builder type — see REPORT.md");
    }

    /**
     * Mapping of the meta-language {@code @@static fromBytes(transactionBytes: bytes)}
     * factory. Stub: the prototype cannot deserialise without a wire format.
     */
    @NonNull
    public static TransactionImpl fromBytes(@NonNull final byte[] transactionBytes) {
        Objects.requireNonNull(transactionBytes, "transactionBytes must not be null");
        throw new UnsupportedOperationException(
                "Transaction.fromBytes is not implemented in the prototype — see REPORT.md");
    }
}
