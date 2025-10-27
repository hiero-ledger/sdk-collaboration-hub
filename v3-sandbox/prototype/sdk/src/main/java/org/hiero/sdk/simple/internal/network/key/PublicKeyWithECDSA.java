package org.hiero.sdk.simple.internal.network.key;


import java.util.Arrays;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

public record PublicKeyWithECDSA(byte[] keyData) implements PublicKey {

    public PublicKeyWithECDSA {
        keyData = Arrays.copyOf(keyData, keyData.length);
    }

    @Override
    public boolean verify(byte[] message, byte[] signature) {
        return KeyUtilitiesECDSA.verify(this, message, signature);
    }

    @Override
    public @NonNull byte[] toBytes(@NonNull KeyEncoding encoding) {
        return KeyUtilitiesECDSA.toBytes(this, encoding);
    }

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ECDSA;
    }
}
