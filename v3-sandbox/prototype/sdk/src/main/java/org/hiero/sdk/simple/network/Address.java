package org.hiero.sdk.simple.network;

import org.jspecify.annotations.NonNull;

public interface Address {

    long shard();

    long realm();

    long num();

    @NonNull String checksum();

    boolean validateChecksum(@NonNull Network network);

    @NonNull
    default String toHumanReadbleString() {
        return shard() + "." + realm() + "." + num();
    }
}
