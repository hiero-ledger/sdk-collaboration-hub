package org.hiero.sdk.simple.network;

import java.util.Arrays;

public record EvmAddress(byte[] bytes) {

    public EvmAddress {
        bytes = Arrays.copyOf(bytes, bytes.length);
    }
}
