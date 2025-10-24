package org.hiero.sdk.simple;

import org.hiero.sdk.simple.network.TransactionId;

public interface Receipt {

    TransactionId transactionId();

    TransactionStatus status();

    ExchangeRate exchangeRate();
}
