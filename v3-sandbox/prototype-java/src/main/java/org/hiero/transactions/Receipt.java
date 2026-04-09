package org.hiero.transactions;

import org.hiero.common.HbarExchangeRate;
import org.jspecify.annotations.NonNull;

/**
 * Receipt of a transaction. Mirrors the meta-language type
 * {@code transactions.Receipt}.
 *
 * <p>The meta-language defines four immutable fields. The Java mapping is an
 * interface so that domain-specific receipt subtypes (e.g.,
 * {@link org.hiero.transactions.accounts.AccountCreateReceipt}) can extend it and
 * add their own immutable fields without forcing implementations to inherit from a
 * concrete record.
 */
public interface Receipt {

    /**
     * @return the id of the transaction
     */
    @NonNull
    TransactionId transactionId();

    /**
     * @return the status of the transaction
     */
    @NonNull
    TransactionStatus status();

    /**
     * @return the exchange rate at the time of the transaction
     */
    @NonNull
    HbarExchangeRate exchangeRate();

    /**
     * @return the next exchange rate
     */
    @NonNull
    HbarExchangeRate nextExchangeRate();
}
