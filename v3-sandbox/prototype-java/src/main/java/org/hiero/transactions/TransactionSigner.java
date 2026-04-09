package org.hiero.transactions;

import org.jspecify.annotations.NonNull;

/**
 * Helper for external signing of transactions. Mirrors the meta-language abstraction
 * {@code transactions.TransactionSigner}.
 *
 * <p>The meta-language abstraction has only a single method, so the Java mapping is
 * a {@link FunctionalInterface} — this lets callers pass a lambda or a
 * {@code java.security.PrivateKey::sign} method reference where appropriate.
 */
@FunctionalInterface
public interface TransactionSigner {

    /**
     * Signs the given transaction bytes and returns the signature.
     *
     * @param transactionBytes the bytes of the transaction to sign
     * @return the signature bytes
     */
    @NonNull
    byte[] signTransaction(@NonNull byte[] transactionBytes);
}
