package org.hiero.sdk.simple.internal;

import java.time.Instant;
import org.hiero.sdk.simple.Record;

public record DefaultRecord(DefaultReceipt receipt, Instant consensusTimestamp) implements
        Record<DefaultReceipt> {

}
