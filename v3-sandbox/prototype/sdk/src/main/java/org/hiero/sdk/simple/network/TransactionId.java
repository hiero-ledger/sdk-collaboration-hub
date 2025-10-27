package org.hiero.sdk.simple.network;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.Receipt;
import org.hiero.sdk.simple.Record;
import org.jspecify.annotations.NonNull;

public record TransactionId(@NonNull AccountId accountId, @NonNull Instant validStart) {

    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;

    private static final long TIMESTAMP_INCREMENT_NANOSECONDS = 1_000L;

    private static final long NANOSECONDS_TO_REMOVE = 10000000000L;

    private static final AtomicLong monotonicTime = new AtomicLong();

    public TransactionId {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(validStart, "validStart must not be null");
    }

    public CompletableFuture<Receipt> queryReceipt(HieroClient client) {
        return client.queryTransactionReceipt(this);
    }

    public Receipt queryReceiptAndWait(HieroClient client, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(unit, "unit must not be null");
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        return queryReceipt(client).get(timeout, unit);
    }

    public Receipt queryReceiptAndWait(HieroClient client)
            throws InterruptedException, ExecutionException, TimeoutException {
        return queryReceiptAndWait(client, client.getDefaultTimeoutInMs(), TimeUnit.SECONDS);
    }


    public CompletableFuture<Record> queryRecord(HieroClient client) {
        return client.queryTransactionRecord(this);
    }

    public Record queryRecordAndWait(HieroClient client, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(unit, "unit must not be null");
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        return queryRecord(client).get(timeout, unit);
    }

    public Record queryRecordAndWait(HieroClient client)
            throws InterruptedException, ExecutionException, TimeoutException {
        return queryRecordAndWait(client, client.getDefaultTimeoutInMs(), TimeUnit.SECONDS);
    }
    
    public String toString() {
        if (accountId != null && validStart != null) {
            return "" + accountId + "@" + validStart.getEpochSecond() + "." + String.format("%09d",
                    validStart.getNano());
        } else {
            throw new IllegalStateException("`TransactionId.toString()` is non-exhaustive");
        }
    }

    @NonNull
    public static TransactionId generate(@NonNull final AccountId accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");

        long currentTime;
        long lastTime;

        // Loop to ensure the generated timestamp is strictly increasing,
        // and it handles the case where the system clock appears to move backward
        // or if multiple threads attempt to generate a timestamp concurrently.
        do {
            // Get the current time in nanoseconds and remove a few seconds to allow for some time drift
            // between the client and the receiving node and prevented spurious INVALID_TRANSACTION_START.
            currentTime = System.currentTimeMillis() * NANOSECONDS_PER_MILLISECOND - NANOSECONDS_TO_REMOVE;

            // Get the last recorded timestamp.
            lastTime = monotonicTime.get();

            // If the current time is less than or equal to the last recorded time,
            // adjust the timestamp to ensure it is strictly increasing.
            if (currentTime <= lastTime) {
                currentTime = lastTime + TIMESTAMP_INCREMENT_NANOSECONDS;
            }
        } while (!monotonicTime.compareAndSet(lastTime, currentTime));

        // NOTE: using ThreadLocalRandom because it's compatible with Android SDK version 26
        return new TransactionId(
                accountId,
                Instant.ofEpochSecond(
                        0, currentTime + ThreadLocalRandom.current().nextLong(1_000)));
    }

    @NonNull
    public static TransactionId from(@NonNull final String transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        final String[] parts = transactionId.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid transaction ID format: " + transactionId);
        }
        final AccountId accountId = AccountId.from(parts[0]);
        final String[] timeParts = parts[1].split("\\.");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("Invalid timestamp format in transaction ID: " + transactionId);
        }
        final long epochSecond = Long.parseLong(timeParts[0]);
        final int nanoAdjustment = Integer.parseInt(timeParts[1]);
        final Instant validStart = Instant.ofEpochSecond(epochSecond, nanoAdjustment);
        return new TransactionId(accountId, validStart);
    }
}
