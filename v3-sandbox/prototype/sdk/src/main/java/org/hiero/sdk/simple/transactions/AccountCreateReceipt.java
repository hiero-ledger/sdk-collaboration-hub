package org.hiero.sdk.simple.transactions;

import org.hiero.sdk.simple.ExchangeRate;
import org.hiero.sdk.simple.Receipt;
import org.hiero.sdk.simple.TransactionStatus;
import org.hiero.sdk.simple.network.AccountId;
import org.hiero.sdk.simple.network.TransactionId;

public record AccountCreateReceipt(TransactionId transactionId,
                                   TransactionStatus status,
                                   ExchangeRate exchangeRate,
                                   AccountId createdAccount) implements Receipt {
}
