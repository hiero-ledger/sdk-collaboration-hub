package org.hiero.transactions.accounts;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.hiero.transactions.Response;
import org.hiero.transactions.TransactionId;
import org.jspecify.annotations.NonNull;

/**
 * Response of an {@code AccountCreate} transaction. Mirrors the meta-language type
 * {@code transactions-accounts.AccountCreateResponse} which is annotated
 * {@code @@finalType}.
 *
 * <p>The async {@code queryReceipt} / {@code queryRecord} methods return a failed
 * {@link CompletionStage} in the prototype: there is no real network roundtrip yet.
 */
public final class AccountCreateResponse implements Response<AccountCreateReceipt, AccountCreateRecord> {

    private final TransactionId transactionId;

    public AccountCreateResponse(@NonNull final TransactionId transactionId) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
    }

    @Override
    @NonNull
    public TransactionId transactionId() {
        return transactionId;
    }

    @Override
    @NonNull
    public CompletionStage<AccountCreateReceipt> queryReceipt() {
        final CompletableFuture<AccountCreateReceipt> future = new CompletableFuture<>();
        future.completeExceptionally(
                new UnsupportedOperationException("queryReceipt is not implemented in the prototype"));
        return future;
    }

    @Override
    @NonNull
    public CompletionStage<AccountCreateRecord> queryRecord() {
        final CompletableFuture<AccountCreateRecord> future = new CompletableFuture<>();
        future.completeExceptionally(
                new UnsupportedOperationException("queryRecord is not implemented in the prototype"));
        return future;
    }
}
