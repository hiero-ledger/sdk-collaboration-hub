package org.hiero.transactions;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.hiero.client.HieroClient;
import org.hiero.common.AccountId;
import org.hiero.keys.KeyPair;
import org.hiero.keys.PublicKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An immutable transaction ready for signing, serialization and submission. Mirrors
 * the meta-language type {@code transactions.Transaction}.
 *
 * <p>The meta-language definition is annotated {@code @@finalType}, so the Java
 * mapping is a {@code final} class — but to keep the prototype's TransactionSPI
 * machinery decoupled from the concrete implementation we expose it as an
 * interface here and provide the {@code final} concrete type as an internal
 * {@code impl} class. The {@code @@finalType} guarantee is preserved by making
 * the only entry points (the static {@link #fromBytes(byte[])} factory and the
 * builder's {@code build(...)} method) return instances of that single
 * {@code final} class — see REPORT.md "Final-type vs interface".
 *
 * <p>The four network execution config fields ({@code maxAttempts},
 * {@code maxBackoff}, {@code minBackoff}, {@code attemptTimeout}) are mutable in the
 * meta-language but explicitly NOT part of the signed body. They are exposed as
 * getters and setters here, all of them taking nullable wrapper types.
 */
public interface Transaction {

    /**
     * @return maximum number of execution attempts, or {@code null} for the default
     */
    @Nullable
    Integer getMaxAttempts();

    /**
     * Sets the maximum number of execution attempts.
     *
     * @param maxAttempts the maximum, or {@code null} to clear
     */
    void setMaxAttempts(@Nullable Integer maxAttempts);

    /**
     * @return maximum backoff in milliseconds, or {@code null} for the default
     */
    @Nullable
    Long getMaxBackoff();

    /**
     * Sets the maximum backoff.
     *
     * @param maxBackoff the maximum in milliseconds, or {@code null} to clear
     */
    void setMaxBackoff(@Nullable Long maxBackoff);

    /**
     * @return minimum backoff in milliseconds, or {@code null} for the default
     */
    @Nullable
    Long getMinBackoff();

    /**
     * Sets the minimum backoff.
     *
     * @param minBackoff the minimum in milliseconds, or {@code null} to clear
     */
    void setMinBackoff(@Nullable Long minBackoff);

    /**
     * @return per-attempt timeout in milliseconds, or {@code null} for the default
     */
    @Nullable
    Long getAttemptTimeout();

    /**
     * Sets the per-attempt timeout.
     *
     * @param attemptTimeout the timeout in milliseconds, or {@code null} to clear
     */
    void setAttemptTimeout(@Nullable Long attemptTimeout);

    /**
     * Signs the transaction with the given key pair.
     *
     * @param keyPair the key pair to sign with
     * @return this transaction (for fluent chaining)
     */
    @NonNull
    Transaction sign(@NonNull KeyPair keyPair);

    /**
     * Signs the transaction using an external signer.
     *
     * @param publicKey         the public key to associate the signature with
     * @param transactionSigner the external signer
     * @return this transaction (for fluent chaining)
     */
    @NonNull
    Transaction sign(@NonNull PublicKey publicKey, @NonNull TransactionSigner transactionSigner);

    /**
     * Returns the signatures that have been added to this transaction, keyed by node
     * account id and public key.
     *
     * @return the signatures map (immutable)
     */
    @NonNull
    Map<AccountId, Map<PublicKey, byte[]>> getSignatures();

    /**
     * Submits the transaction to the network and returns the response.
     *
     * @param client the client to submit through
     * @return a {@link CompletionStage} that completes with the response
     */
    @NonNull
    CompletionStage<Response<? extends Receipt, ? extends Record<? extends Receipt>>> execute(
            @NonNull HieroClient client);

    /**
     * Serialises the transaction (including all signatures so far) to bytes.
     *
     * @return the serialised bytes
     */
    @NonNull
    byte[] toBytes();

    /**
     * Returns a fresh mutable builder pre-populated with this transaction's body.
     *
     * @return a builder for the same transaction body
     */
    @NonNull
    TransactionBuilder<?, ?, ?, ?> unbuild();

    /**
     * Deserialises a transaction from bytes.
     *
     * @param transactionBytes the bytes to deserialise
     * @return the deserialised transaction
     */
    @NonNull
    static Transaction fromBytes(@NonNull final byte[] transactionBytes) {
        return org.hiero.transactions.impl.TransactionImpl.fromBytes(transactionBytes);
    }
}
