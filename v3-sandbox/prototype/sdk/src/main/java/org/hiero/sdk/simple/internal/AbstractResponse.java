package org.hiero.sdk.simple.internal;

import com.hedera.hashgraph.sdk.proto.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.TransactionRecord;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.Receipt;
import org.hiero.sdk.simple.Record;
import org.hiero.sdk.simple.Response;
import org.hiero.sdk.simple.network.TransactionId;
import org.jspecify.annotations.NonNull;

public abstract class AbstractResponse<RECEIPT extends Receipt, RECORD extends Record<RECEIPT>> implements
        Response<RECEIPT, RECORD> {

    private final HieroClient client;

    private final TransactionId transactionId;

    private final BiFunction<TransactionId, TransactionReceipt, RECEIPT> receiptFactory;

    private final BiFunction<TransactionId, TransactionRecord, RECORD> recordFactory;

    public AbstractResponse(@NonNull final HieroClient client,
            @NonNull final TransactionId transactionId,
            @NonNull final BiFunction<TransactionId, TransactionReceipt, RECEIPT> receiptFactory,
            @NonNull final BiFunction<TransactionId, TransactionRecord, RECORD> recordFactory) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.receiptFactory = Objects.requireNonNull(receiptFactory, "receiptFactory must not be null");
        this.recordFactory = Objects.requireNonNull(recordFactory, "recordFactory must not be null");
    }

    @Override
    public TransactionId transactionId() {
        return transactionId;
    }

    @Override
    public CompletableFuture<RECEIPT> queryReceipt() {
        return client.queryTransactionReceipt(transactionId, receiptFactory);
    }

    @Override
    public CompletableFuture<RECORD> queryRecord() {
        return client.queryTransactionRecord(transactionId, recordFactory);
    }

    @Override
    public RECORD queryRecordAndWait() throws InterruptedException, ExecutionException, TimeoutException {
        final long defaultTimeoutInMs = client.getDefaultTimeoutInMs();
        return queryRecordAndWait(defaultTimeoutInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public RECEIPT queryReceiptAndWait()
            throws InterruptedException, ExecutionException, TimeoutException {
        final long defaultTimeoutInMs = client.getDefaultTimeoutInMs();
        return queryReceiptAndWait(defaultTimeoutInMs, TimeUnit.MILLISECONDS);
    }
}
