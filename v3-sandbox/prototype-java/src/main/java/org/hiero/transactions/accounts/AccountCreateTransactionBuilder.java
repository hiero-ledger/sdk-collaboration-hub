package org.hiero.transactions.accounts;

import java.util.Objects;
import org.hiero.client.HieroClient;
import org.hiero.common.Hbar;
import org.hiero.common.HbarUnit;
import org.hiero.keys.PublicKey;
import org.hiero.transactions.impl.AbstractTransactionBuilder;
import org.hiero.transactions.impl.TransactionImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Builder for an {@code AccountCreate} transaction. Mirrors the meta-language type
 * {@code transactions-accounts.AccountCreateTransactionBuilder} which is annotated
 * {@code @@finalType} — the Java mapping is therefore a {@code final} class.
 */
public final class AccountCreateTransactionBuilder extends AbstractTransactionBuilder<
        AccountCreateTransactionBuilder,
        AccountCreateReceipt,
        AccountCreateRecord,
        AccountCreateResponse> {

    @Nullable
    private String accountMemo;

    @NonNull
    private Hbar initialBalance = new Hbar(0L, HbarUnit.HBAR);

    @Nullable
    private PublicKey key;

    @NonNull
    @Override
    protected AccountCreateTransactionBuilder self() {
        return this;
    }

    /**
     * @return the optional account memo
     */
    @Nullable
    public String getAccountMemo() {
        return accountMemo;
    }

    /**
     * Sets the optional account memo.
     *
     * @param accountMemo the memo or {@code null} to clear
     * @return this builder for fluent chaining
     */
    @NonNull
    public AccountCreateTransactionBuilder setAccountMemo(@Nullable final String accountMemo) {
        this.accountMemo = accountMemo;
        return this;
    }

    /**
     * @return the initial balance for the new account (defaults to 0)
     */
    @NonNull
    public Hbar getInitialBalance() {
        return initialBalance;
    }

    /**
     * Sets the initial balance for the new account. Mirrors the meta-language
     * {@code @@default(0)} on the {@code initialBalance} field — the field can never
     * be {@code null}.
     *
     * @param initialBalance the initial balance, never {@code null}
     * @return this builder for fluent chaining
     */
    @NonNull
    public AccountCreateTransactionBuilder setInitialBalance(@NonNull final Hbar initialBalance) {
        Objects.requireNonNull(initialBalance, "initialBalance must not be null");
        this.initialBalance = initialBalance;
        return this;
    }

    /**
     * @return the public key associated with the account, or {@code null} if not yet set
     */
    @Nullable
    public PublicKey getKey() {
        return key;
    }

    /**
     * Sets the public key associated with the new account. Mirrors the meta-language
     * field {@code key:keys.PublicKey} which is not nullable — the value must be
     * provided before {@code build()} is called.
     *
     * @param key the public key, never {@code null}
     * @return this builder for fluent chaining
     */
    @NonNull
    public AccountCreateTransactionBuilder setKey(@NonNull final PublicKey key) {
        Objects.requireNonNull(key, "key must not be null");
        this.key = key;
        return this;
    }

    @Override
    @NonNull
    protected Object buildDomainSpecificBody() {
        if (key == null) {
            throw new IllegalStateException("key must be set before build()");
        }
        return new AccountCreateBody(accountMemo, initialBalance, key);
    }

    @Override
    @NonNull
    protected AccountCreateResponse createResponse(
            @NonNull final TransactionImpl transaction,
            @NonNull final HieroClient client) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        Objects.requireNonNull(client, "client must not be null");
        if (transaction.body().transactionId() == null) {
            throw new IllegalStateException("Transaction id must be set before execution");
        }
        return new AccountCreateResponse(transaction.body().transactionId());
    }

    /**
     * Internal record carrying the domain-specific fields of an account create body.
     * Visible only inside this package — see REPORT.md for the discussion of how
     * the SPI layer would convert this to/from protobuf in a real implementation.
     */
    record AccountCreateBody(
            @Nullable String accountMemo,
            @NonNull Hbar initialBalance,
            @NonNull PublicKey key) {

        AccountCreateBody {
            Objects.requireNonNull(initialBalance, "initialBalance must not be null");
            Objects.requireNonNull(key, "key must not be null");
        }
    }
}
