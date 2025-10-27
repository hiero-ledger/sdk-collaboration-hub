package org.hiero.sdk.simple;

import java.time.Instant;
import org.hiero.sdk.simple.network.TransactionId;

public interface Record<R extends Receipt> {

    default TransactionId transactionId() {
        return receipt().transactionId();
    }

    R receipt();

    Instant consensusTimestamp();
}
