package org.hiero.transactions.accounts;

import java.util.Objects;
import org.hiero.common.AccountId;
import org.hiero.common.HbarExchangeRate;
import org.hiero.transactions.Receipt;
import org.hiero.transactions.TransactionId;
import org.hiero.transactions.TransactionStatus;
import org.jspecify.annotations.NonNull;

/**
 * Receipt of an {@code AccountCreate} transaction. Mirrors the meta-language type
 * {@code transactions-accounts.AccountCreateReceipt} which is annotated
 * {@code @@finalType} — the Java mapping is therefore a {@code record} (records are
 * implicitly final).
 *
 * @param transactionId    the id of the transaction
 * @param status           the status of the transaction
 * @param exchangeRate     the exchange rate at the time of the transaction
 * @param nextExchangeRate the next exchange rate
 * @param accountId        the id of the freshly created account
 */
public record AccountCreateReceipt(
        @NonNull TransactionId transactionId,
        @NonNull TransactionStatus status,
        @NonNull HbarExchangeRate exchangeRate,
        @NonNull HbarExchangeRate nextExchangeRate,
        @NonNull AccountId accountId) implements Receipt {

    public AccountCreateReceipt {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(exchangeRate, "exchangeRate must not be null");
        Objects.requireNonNull(nextExchangeRate, "nextExchangeRate must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
    }
}
