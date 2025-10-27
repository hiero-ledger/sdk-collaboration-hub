package org.hiero.sdk.simple.transactions;

import java.time.Instant;
import org.hiero.sdk.simple.Record;
import org.hiero.sdk.simple.network.AccountId;

public record AccountCreateRecord(AccountCreateReceipt receipt, Instant consensusTimestamp) implements
        Record<AccountCreateReceipt> {

    public AccountId createdAccount() {
        return receipt.createdAccount();
    }
}
