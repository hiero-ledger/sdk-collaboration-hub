package org.hiero.transactions.impl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hiero.client.HieroClient;
import org.hiero.client.OperatorAccount;
import org.hiero.common.AccountId;
import org.hiero.common.ConsensusNode;
import org.hiero.common.Hbar;
import org.hiero.transactions.Receipt;
import org.hiero.transactions.Record;
import org.hiero.transactions.Response;
import org.hiero.transactions.Transaction;
import org.hiero.transactions.TransactionBuilder;
import org.hiero.transactions.TransactionId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reusable base class for concrete {@link TransactionBuilder} implementations. The
 * generic parameters mirror the public {@link TransactionBuilder} interface.
 *
 * <p>Subclasses are expected to:
 * <ul>
 *   <li>Add their own domain-specific setters that return {@code self()} for fluent
 *   chaining.</li>
 *   <li>Implement {@link #buildDomainSpecificBody()} to produce the domain-specific
 *   body snapshot that will be embedded in {@link TransactionBody}.</li>
 *   <li>Implement {@link #createResponse(TransactionImpl, HieroClient)} to wire the
 *   produced {@link TransactionImpl} into a domain-specific {@link Response}.</li>
 * </ul>
 *
 * @param <SELF>    the concrete builder subclass for fluent chaining (CRTP)
 * @param <RECEIPT> the receipt subclass produced by the response
 * @param <RECORD>  the record subclass produced by the response
 * @param <RESP>    the concrete response subclass
 */
public abstract class AbstractTransactionBuilder<
        SELF extends AbstractTransactionBuilder<SELF, RECEIPT, RECORD, RESP>,
        RECEIPT extends Receipt,
        RECORD extends Record<RECEIPT>,
        RESP extends Response<RECEIPT, RECORD>>
        implements TransactionBuilder<SELF, RECEIPT, RECORD, RESP> {

    @Nullable
    private Hbar maxTransactionFee;

    @Nullable
    private Long validDurationSeconds;

    @Nullable
    private String memo;

    @Nullable
    private TransactionId transactionId;

    private final List<AccountId> nodeAccountIds = new CopyOnWriteArrayList<>();

    /**
     * Returns {@code this} typed as the concrete CRTP type. Subclasses simply
     * implement {@code @return (SELF) this;}.
     */
    @NonNull
    protected abstract SELF self();

    /**
     * Builds the domain-specific body that will be stored in the {@link TransactionBody}
     * snapshot. The returned object is opaque to the framework — concrete
     * {@code TransactionSupport} implementations downcast it to their own type.
     */
    @NonNull
    protected abstract Object buildDomainSpecificBody();

    /**
     * Wraps a freshly built {@link TransactionImpl} into a domain-specific
     * {@link Response}. Concrete subclasses know which response class to construct.
     */
    @NonNull
    protected abstract RESP createResponse(@NonNull TransactionImpl transaction, @NonNull HieroClient client);

    @Override
    @Nullable
    public Hbar getMaxTransactionFee() {
        return maxTransactionFee;
    }

    @Override
    @NonNull
    public SELF setMaxTransactionFee(@Nullable final Hbar maxTransactionFee) {
        this.maxTransactionFee = maxTransactionFee;
        return self();
    }

    @Override
    @Nullable
    public Long getValidDuration() {
        return validDurationSeconds;
    }

    @Override
    @NonNull
    public SELF setValidDuration(@Nullable final Long validDurationSeconds) {
        this.validDurationSeconds = validDurationSeconds;
        return self();
    }

    @Override
    @Nullable
    public String getMemo() {
        return memo;
    }

    @Override
    @NonNull
    public SELF setMemo(@Nullable final String memo) {
        this.memo = memo;
        return self();
    }

    @Override
    @Nullable
    public TransactionId getTransactionId() {
        return transactionId;
    }

    @Override
    @NonNull
    public SELF setTransactionId(@Nullable final TransactionId transactionId) {
        this.transactionId = transactionId;
        return self();
    }

    @Override
    @NonNull
    public List<AccountId> getNodeAccountIds() {
        return List.copyOf(nodeAccountIds);
    }

    @Override
    @NonNull
    public SELF setNodeAccountIds(@NonNull final List<AccountId> nodeAccountIds) {
        Objects.requireNonNull(nodeAccountIds, "nodeAccountIds must not be null");
        this.nodeAccountIds.clear();
        this.nodeAccountIds.addAll(nodeAccountIds);
        return self();
    }

    @Override
    @NonNull
    public Transaction build(@Nullable final HieroClient client) {
        TransactionId effectiveTxId = this.transactionId;
        List<AccountId> effectiveNodes = List.copyOf(this.nodeAccountIds);
        if (client != null) {
            if (effectiveTxId == null) {
                final OperatorAccount operator = client.getOperatorAccount();
                effectiveTxId = TransactionId.generateTransactionId(operator.accountId());
            }
            if (effectiveNodes.isEmpty() && client instanceof org.hiero.client.impl.HieroClientImpl impl) {
                effectiveNodes = impl.getNetworkSetting().getConsensusNodes()
                        .stream()
                        .map(ConsensusNode::account)
                        .toList();
            }
        }
        final TransactionBody body = new TransactionBody(
                maxTransactionFee,
                validDurationSeconds,
                memo,
                effectiveTxId,
                effectiveNodes,
                buildDomainSpecificBody());
        return new TransactionImpl(body);
    }

    @Override
    @NonNull
    public CompletionStage<RESP> buildAndExecute(@NonNull final HieroClient client) {
        Objects.requireNonNull(client, "client must not be null");
        final TransactionImpl tx = (TransactionImpl) build(client);
        // Sign with the operator. Per the meta-language semantics buildAndExecute
        // is the single-signer convenience flow.
        final org.hiero.keys.PrivateKey operatorPrivate = client.getOperatorAccount().privateKey();
        final org.hiero.keys.KeyPair operatorKeyPair =
                new org.hiero.keys.KeyPair(operatorPrivate.createPublicKey(), operatorPrivate);
        tx.sign(operatorKeyPair);
        // The prototype does not actually transmit the transaction (see REPORT.md).
        // Returning a freshly created Response keeps the API contract intact.
        final RESP response = createResponse(tx, client);
        return CompletableFuture.completedFuture(response);
    }
}
