package org.hiero.sdk.simple;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hiero.sdk.simple.network.TransactionId;

public interface Response<RECEIPT extends Receipt, RECORD extends Record<RECEIPT>> {

    TransactionId transactionId();

    CompletableFuture<RECEIPT> queryReceipt();

    default RECEIPT queryReceiptAndWait(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(unit, "unit must not be null");
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        return queryReceipt().get(timeout, unit);
    }

    RECEIPT queryReceiptAndWait() throws InterruptedException, ExecutionException, TimeoutException;


    CompletableFuture<RECORD> queryRecord();

    default RECORD queryRecordAndWait(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(unit, "unit must not be null");
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        return queryRecord().get(timeout, unit);
    }

    RECORD queryRecordAndWait() throws InterruptedException, ExecutionException, TimeoutException;
}
