package org.hiero.keys.impl;

import static org.hiero.keys.impl.Oids.ID_ED25519;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.hiero.keys.Key;
import org.hiero.keys.KeyAlgorithm;
import org.hiero.keys.KeyType;
import org.hiero.keys.PrivateKey;
import org.hiero.keys.PublicKey;
import org.hiero.keys.io.KeyEncoding;
import org.hiero.keys.io.KeyFormat;
import org.hiero.keys.io.RawFormat;
import org.jspecify.annotations.NonNull;

/**
 * Ed25519 private key implementation. Internal class — instances are obtained via
 * the public factory methods on {@link PrivateKey}.
 */
public final class Ed25519PrivateKey implements PrivateKey {

    private final byte[] seed32;

    Ed25519PrivateKey(@NonNull final byte[] seed32) {
        Objects.requireNonNull(seed32, "seed32 must not be null");
        if (seed32.length != 32) {
            throw new IllegalArgumentException("Ed25519 private key must be 32 bytes");
        }
        this.seed32 = seed32.clone();
    }

    @Override
    @NonNull
    public byte[] sign(@NonNull final byte[] message) {
        Objects.requireNonNull(message, "message must not be null");
        final byte[] sig = new byte[Ed25519.SIGNATURE_SIZE];
        Ed25519.sign(seed32, 0, message, 0, message.length, sig, 0);
        return sig;
    }

    @Override
    @NonNull
    public PublicKey createPublicKey() {
        final byte[] pub = new byte[Ed25519.PUBLIC_KEY_SIZE];
        Ed25519.generatePublicKey(seed32, 0, pub, 0);
        return new Ed25519PublicKey(pub);
    }

    @Override
    @NonNull
    public byte[] toRawBytes() {
        return seed32.clone();
    }

    @Override
    @NonNull
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ED25519;
    }

    @Override
    @NonNull
    public KeyType type() {
        return KeyType.PRIVATE;
    }

    @Override
    @NonNull
    public byte[] toBytes(@NonNull final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (!container.supportsType(KeyType.PRIVATE)) {
            throw new IllegalArgumentException("Container does not support private keys: " + container);
        }
        if (container.getRawFormat() != RawFormat.BYTES) {
            throw new IllegalArgumentException("toBytes requires BYTES format: " + container);
        }
        try {
            if (container.getEncoding() == KeyEncoding.DER) {
                final PrivateKeyInfo pki = new PrivateKeyInfo(
                        new AlgorithmIdentifier(ID_ED25519),
                        new DEROctetString(seed32));
                return pki.getEncoded("DER");
            }
            throw new IllegalArgumentException("Unsupported container/format for Ed25519 private key: " + container);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error encoding Ed25519 private key", e);
        }
    }

    @Override
    @NonNull
    public String toString(@NonNull final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (container.getRawFormat() != RawFormat.STRING) {
            throw new IllegalArgumentException("Requested String for non-STRING container: " + container);
        }
        if (container == KeyFormat.PKCS8_WITH_PEM) {
            return PemUtil.toPem("PRIVATE KEY", toBytes(KeyFormat.PKCS8_WITH_DER));
        }
        throw new IllegalArgumentException("Unsupported container for toString: " + container);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Key k)) {
            return false;
        }
        return type() == k.type()
                && algorithm() == k.algorithm()
                && Arrays.equals(toRawBytes(), k.toRawBytes());
    }

    @Override
    public int hashCode() {
        int result = type().hashCode();
        result = 31 * result + algorithm().hashCode();
        result = 31 * result + Arrays.hashCode(toRawBytes());
        return result;
    }
}
