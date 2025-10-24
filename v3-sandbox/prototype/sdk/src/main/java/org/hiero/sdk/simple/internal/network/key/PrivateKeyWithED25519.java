package org.hiero.sdk.simple.internal.network.key;

import java.util.Arrays;
import org.bouncycastle.crypto.params.KeyParameter;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

public record PrivateKeyWithED25519(byte[] keyData, KeyParameter keyParameter) implements PrivateKey {

    public PrivateKeyWithED25519 {
        keyData = Arrays.copyOf(keyData, keyData.length);
    }

    @Override
    public PublicKey createPublicKey() {
        return KeyUtilitiesED25519.createPublicKey(this);
    }

    @Override
    public byte[] sign(byte[] message) {
        return KeyUtilitiesED25519.sign(this, message);
    }

    @Override
    public @NonNull byte[] toBytes(@NonNull KeyEncoding encoding) {
        return KeyUtilitiesED25519.toBytes(this, encoding);
    }

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ED25519;
    }
}
