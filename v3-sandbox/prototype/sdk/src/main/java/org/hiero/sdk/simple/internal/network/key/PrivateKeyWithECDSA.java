package org.hiero.sdk.simple.internal.network.key;

import java.math.BigInteger;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

public record PrivateKeyWithECDSA(BigInteger keyData) implements PrivateKey {

    @Override
    public PublicKey createPublicKey() {
        return KeyUtilitiesECDSA.createPublicKey(this);
    }

    @Override
    public byte[] sign(byte[] message) {
        return KeyUtilitiesECDSA.sign(this, message);
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
