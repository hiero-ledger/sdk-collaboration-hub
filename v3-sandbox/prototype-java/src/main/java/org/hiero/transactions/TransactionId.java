package org.hiero.transactions;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.hiero.common.AccountId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Transaction id. Mirrors the meta-language abstraction {@code transactions.TransactionId}.
 *
 * <p>The meta-language declares this as an abstraction with three immutable fields
 * (one of them nullable). All concrete fields are immutable so the Java mapping is
 * a {@code record} that implements an interface — actually, since there is only one
 * obvious shape, the Java type is a record directly.
 *
 * @param accountId  the account that is the payer of the transaction
 * @param validStart the start time of the transaction
 * @param nonce      the nonce of an internal transaction or {@code null}
 */
public record TransactionId(
        @NonNull AccountId accountId,
        @NonNull ZonedDateTime validStart,
        @Nullable Integer nonce) {

    /**
     * Monotonic counter used to ensure {@link #generateTransactionId(AccountId)}
     * never produces a duplicate timestamp under concurrent calls.
     */
    private static final AtomicLong MONOTONIC_NANOS = new AtomicLong();

    private static final long NANOS_PER_MILLISECOND = 1_000_000L;

    /**
     * Skew the transaction id slightly into the past to give the consensus node a
     * window in which to accept it. This mirrors the same behavior as the existing
     * V2 SDK.
     */
    private static final long PAST_SKEW_NANOS = 10_000_000_000L;

    private static final long MIN_INCREMENT_NANOS = 1_000L;

    public TransactionId {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(validStart, "validStart must not be null");
    }

    /**
     * Convenience constructor without a nonce.
     */
    public TransactionId(@NonNull final AccountId accountId, @NonNull final ZonedDateTime validStart) {
        this(accountId, validStart, null);
    }

    /**
     * @return the transaction id as a {@code accountId@validStart} string
     */
    @Override
    @NonNull
    public String toString() {
        return accountId + "@" + validStart.toEpochSecond() + "."
                + String.format("%09d", validStart.getNano());
    }

    /**
     * @return the transaction id with the account id checksum included
     */
    @NonNull
    public String toStringWithChecksum() {
        return accountId.toStringWithChecksum() + "@" + validStart.toEpochSecond() + "."
                + String.format("%09d", validStart.getNano());
    }

    /**
     * Generates a new {@link TransactionId} for the given payer account. The
     * {@code validStart} is monotonically increasing across concurrent invocations.
     *
     * @param accountId the payer account id
     * @return the new transaction id
     */
    @NonNull
    public static TransactionId generateTransactionId(@NonNull final AccountId accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        long nextNanos;
        long previous;
        do {
            nextNanos = System.currentTimeMillis() * NANOS_PER_MILLISECOND - PAST_SKEW_NANOS;
            previous = MONOTONIC_NANOS.get();
            if (nextNanos <= previous) {
                nextNanos = previous + MIN_INCREMENT_NANOS;
            }
        } while (!MONOTONIC_NANOS.compareAndSet(previous, nextNanos));
        final long jitter = ThreadLocalRandom.current().nextLong(1_000L);
        final long epochSeconds = Math.floorDiv(nextNanos + jitter, 1_000_000_000L);
        final int nano = (int) Math.floorMod(nextNanos + jitter, 1_000_000_000L);
        return new TransactionId(
                accountId,
                ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(epochSeconds, nano), ZoneOffset.UTC));
    }

    /**
     * Parses a transaction id in {@code accountId@seconds.nanoseconds} format.
     *
     * @param transactionId the string to parse
     * @return the parsed {@link TransactionId}
     * @throws IllegalArgumentException Java mapping of {@code @@throws(illegal-format)}
     */
    @NonNull
    public static TransactionId fromString(@NonNull final String transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        final String[] parts = transactionId.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid transaction id: " + transactionId);
        }
        final AccountId accountId = AccountId.fromString(parts[0]);
        final String[] timeParts = parts[1].split("\\.");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("Invalid transaction id timestamp: " + transactionId);
        }
        final long seconds;
        final int nanos;
        try {
            seconds = Long.parseLong(timeParts[0]);
            nanos = Integer.parseInt(timeParts[1]);
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid transaction id timestamp: " + transactionId, ex);
        }
        return new TransactionId(
                accountId,
                ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(seconds, nanos), ZoneOffset.UTC));
    }
}
