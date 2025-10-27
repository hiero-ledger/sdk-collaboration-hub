package org.hiero.sdk.simple.transactions;

import java.util.Objects;
import org.hiero.sdk.simple.internal.AbstractTransaction;
import org.hiero.sdk.simple.network.Hbar;
import org.hiero.sdk.simple.network.keys.Key;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AccountCreateTransaction extends
        AbstractTransaction<AccountCreateResponse, AccountCreateTransaction> {

    private String accountMemo = "";

    private Hbar initialBalance = Hbar.ZERO;

    private Key key;

    @NonNull
    @Override
    protected AccountCreateTransaction self() {
        return this;
    }
    
    @Nullable
    public String getAccountMemo() {
        return accountMemo;
    }

    public void setAccountMemo(final @NonNull String accountMemo) {
        Objects.requireNonNull(accountMemo, "accountMemo must not be null");
        this.accountMemo = accountMemo;
    }

    @NonNull
    public AccountCreateTransaction withAccountMemo(final @Nullable String accountMemo) {
        setAccountMemo(accountMemo);
        return self();
    }

    @Nullable
    public Hbar getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(final @NonNull Hbar initialBalance) {
        Objects.requireNonNull(initialBalance, "initialBalance must not be null");
        this.initialBalance = initialBalance;
    }

    @NonNull
    public AccountCreateTransaction withInitialBalance(final @Nullable Hbar initialBalance) {
        setInitialBalance(initialBalance);
        return self();
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    @NonNull
    public AccountCreateTransaction withKey(@NonNull Key key) {
        setKey(key);
        return self();
    }
}
