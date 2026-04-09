package org.hiero.transactions;

import java.time.ZonedDateTime;
import org.jspecify.annotations.NonNull;

/**
 * Record of a transaction. Mirrors the meta-language generic type
 * {@code transactions.Record<$$Receipt extends Receipt>}.
 *
 * <p>The meta-language definition uses a self-bounded generic to give domain-specific
 * record subtypes access to a more specific receipt type at compile time. The Java
 * mapping drops the {@code $$} prefix per the api-best-practices guide.
 *
 * @param <R> the receipt type
 */
public interface Record<R extends Receipt> {

    /**
     * @return the id of the transaction
     */
    @NonNull
    TransactionId transactionId();

    /**
     * @return the consensus time of the transaction
     */
    @NonNull
    ZonedDateTime consensusTimestamp();

    /**
     * @return the receipt of the transaction
     */
    @NonNull
    R receipt();
}
