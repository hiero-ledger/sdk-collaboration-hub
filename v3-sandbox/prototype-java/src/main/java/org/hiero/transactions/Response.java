package org.hiero.transactions;

import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.NonNull;

/**
 * Response of a transaction execution. Mirrors the meta-language generic type
 * {@code transactions.Response<$$Receipt extends Receipt, $$Record extends Record>}.
 *
 * <p>The two {@code @@async} methods map to {@link CompletionStage} per
 * api-best-practices-java.md "Asynchronous methods".
 *
 * @param <R>   the receipt subtype
 * @param <REC> the record subtype
 */
public interface Response<R extends Receipt, REC extends Record<R>> {

    /**
     * @return the id of the transaction
     */
    @NonNull
    TransactionId transactionId();

    /**
     * Asynchronously queries the receipt of the transaction.
     *
     * @return a {@link CompletionStage} that completes with the receipt
     */
    @NonNull
    CompletionStage<R> queryReceipt();

    /**
     * Asynchronously queries the record of the transaction.
     *
     * @return a {@link CompletionStage} that completes with the record
     */
    @NonNull
    CompletionStage<REC> queryRecord();
}
