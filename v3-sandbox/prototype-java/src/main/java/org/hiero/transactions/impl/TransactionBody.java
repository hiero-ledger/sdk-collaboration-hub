package org.hiero.transactions.impl;

import java.util.List;
import java.util.Objects;
import org.hiero.common.AccountId;
import org.hiero.common.Hbar;
import org.hiero.transactions.TransactionId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Internal representation of the immutable body of a transaction.
 *
 * <p>The meta-language defines the body fields on {@link org.hiero.transactions.TransactionBuilder}
 * (the mutable side) and the {@code @@finalType Transaction} contains only the
 * network execution config. This record is the implementation-only snapshot used to
 * carry the body across the build/sign/execute boundary.
 *
 * @param maxTransactionFee   the maximum transaction fee or {@code null}
 * @param validDurationSeconds the valid duration in seconds or {@code null}
 * @param memo                the memo or {@code null}
 * @param transactionId       the transaction id or {@code null} (HIP-745 flow)
 * @param nodeAccountIds      the node account ids (never {@code null}, may be empty)
 * @param domainSpecificBody  the domain-specific body data (e.g., the
 *                            {@code AccountCreateBody})
 */
public record TransactionBody(
        @Nullable Hbar maxTransactionFee,
        @Nullable Long validDurationSeconds,
        @Nullable String memo,
        @Nullable TransactionId transactionId,
        @NonNull List<AccountId> nodeAccountIds,
        @NonNull Object domainSpecificBody) {

    public TransactionBody {
        Objects.requireNonNull(nodeAccountIds, "nodeAccountIds must not be null");
        Objects.requireNonNull(domainSpecificBody, "domainSpecificBody must not be null");
        nodeAccountIds = List.copyOf(nodeAccountIds);
    }
}
