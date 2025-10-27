package org.hiero.sdk.simple;

import java.time.Duration;
import org.hiero.sdk.simple.network.Hbar;
import org.jspecify.annotations.NonNull;

/**
 * Represents a transaction that can be sent to a Hiero network.
 *
 * @param <T> the type of the transaction
 * @param <R> the type of the response
 */
public interface Transaction<T extends Transaction, R extends Response> {

    Hbar getFee();

    void setFee(Hbar fee);

    T withFee(Hbar fee);

    Duration getValidDuration();

    void setValidDuration(Duration validDuration);

    T withValidDuration(Duration validDuration);

    String getMemo();

    void setMemo(String memo);

    T withMemo(String memo);

    /**
     * Returns a frozen representation the transaction. This step is needed to prepare the transaction for sending to a
     * Hiero network. The frozen transaction is immutable and a new instance is created each time this method is
     * called.
     *
     * @param client the Hiero client used to freeze the transaction
     * @return a {@link PackedTransaction} representing the frozen state of this transaction
     */
    PackedTransaction<T, R> packTransaction(@NonNull HieroClient client);
}
