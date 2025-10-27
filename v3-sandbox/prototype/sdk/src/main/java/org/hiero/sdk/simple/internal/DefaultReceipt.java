package org.hiero.sdk.simple.internal;

import org.hiero.sdk.simple.ExchangeRate;
import org.hiero.sdk.simple.Receipt;
import org.hiero.sdk.simple.TransactionStatus;
import org.hiero.sdk.simple.network.TransactionId;

public record DefaultReceipt(TransactionId transactionId, TransactionStatus status,
                             ExchangeRate exchangeRate) implements Receipt {

}
