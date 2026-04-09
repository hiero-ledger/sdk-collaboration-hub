package org.hiero.transactions;

import java.util.List;
import java.util.concurrent.CompletionStage;
import org.hiero.client.HieroClient;
import org.hiero.common.AccountId;
import org.hiero.common.Hbar;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Mutable builder for constructing a transaction body. Mirrors the meta-language
 * abstraction
 * {@code transactions.TransactionBuilder<$$Transaction extends TransactionBuilder, $$Response extends Response>}.
 *
 * <p>The meta-language uses a self-referential generic (CRTP) so that fluent setter
 * chains return the concrete builder type. The Java mapping uses the same pattern.
 *
 * <p>The meta-language definition's generic parameter list is slightly inconsistent —
 * the {@code $$Transaction} parameter is named after the builder type, not the
 * built {@link Transaction} — see REPORT.md "TransactionBuilder generic naming". The
 * Java type uses descriptive parameter names that make the intent clearer:
 * <ul>
 *   <li>{@code SELF}     — the concrete builder subclass (CRTP)</li>
 *   <li>{@code RESP}     — the concrete {@link Response} subclass</li>
 *   <li>{@code RECEIPT}  — the receipt subclass produced by the response</li>
 *   <li>{@code RECORD}   — the record subclass produced by the response</li>
 * </ul>
 *
 * @param <SELF>    the concrete builder subclass for fluent chaining (CRTP)
 * @param <RECEIPT> the receipt subclass produced by the response
 * @param <RECORD>  the record subclass produced by the response
 * @param <RESP>    the concrete response subclass
 */
public interface TransactionBuilder<
        SELF extends TransactionBuilder<SELF, RECEIPT, RECORD, RESP>,
        RECEIPT extends Receipt,
        RECORD extends Record<RECEIPT>,
        RESP extends Response<RECEIPT, RECORD>> {

    @Nullable
    Hbar getMaxTransactionFee();

    @NonNull
    SELF setMaxTransactionFee(@Nullable Hbar maxTransactionFee);

    @Nullable
    Long getValidDuration();

    @NonNull
    SELF setValidDuration(@Nullable Long validDurationSeconds);

    @Nullable
    String getMemo();

    @NonNull
    SELF setMemo(@Nullable String memo);

    @Nullable
    TransactionId getTransactionId();

    @NonNull
    SELF setTransactionId(@Nullable TransactionId transactionId);

    /**
     * Returns the list of node account ids the transaction will be sent to. The
     * meta-language definition explicitly does NOT mark this as nullable — empty
     * list is the unset state.
     *
     * @return the list of node account ids (never {@code null})
     */
    @NonNull
    List<AccountId> getNodeAccountIds();

    /**
     * Replaces the list of node account ids.
     *
     * @param nodeAccountIds the new list (must not be {@code null}; pass an empty
     *                       list to clear)
     * @return this builder for fluent chaining
     */
    @NonNull
    SELF setNodeAccountIds(@NonNull List<AccountId> nodeAccountIds);

    /**
     * Transitions from the build phase to the sign/send phase. If a client is
     * provided, {@code transactionId} and {@code nodeAccountIds} are auto-generated
     * from the client. If no client is provided, they are left unset (HIP-745
     * incomplete-transaction flow).
     *
     * @param client the client to derive defaults from, or {@code null}
     * @return the immutable {@link Transaction}
     */
    @NonNull
    Transaction build(@Nullable HieroClient client);

    /**
     * Convenience for simple single-signer flows. Internally calls {@code build(client)},
     * signs with the operator, and executes the transaction.
     *
     * @param client the client to use
     * @return a {@link CompletionStage} that completes with the response
     */
    @NonNull
    CompletionStage<RESP> buildAndExecute(@NonNull HieroClient client);
}
