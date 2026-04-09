package org.hiero.transactions.accounts;

import java.time.ZonedDateTime;
import java.util.Objects;
import org.hiero.common.AccountId;
import org.hiero.transactions.Record;
import org.hiero.transactions.TransactionId;
import org.jspecify.annotations.NonNull;

/**
 * Record of an {@code AccountCreate} transaction. Mirrors the meta-language type
 * {@code transactions-accounts.AccountCreateRecord} which is annotated
 * {@code @@finalType}.
 */
public record AccountCreateRecord(
        @NonNull TransactionId transactionId,
        @NonNull ZonedDateTime consensusTimestamp,
        @NonNull AccountCreateReceipt receipt,
        @NonNull AccountId accountId) implements Record<AccountCreateReceipt> {

    public AccountCreateRecord {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(consensusTimestamp, "consensusTimestamp must not be null");
        Objects.requireNonNull(receipt, "receipt must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
    }
}
