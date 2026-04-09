package org.hiero.keys.impl;

import static org.hiero.keys.impl.Oids.ID_ED25519;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.hiero.keys.Key;
import org.hiero.keys.KeyAlgorithm;
import org.hiero.keys.KeyType;
import org.hiero.keys.PublicKey;
import org.hiero.keys.io.KeyEncoding;
import org.hiero.keys.io.KeyFormat;
import org.hiero.keys.io.RawFormat;
import org.jspecify.annotations.NonNull;

/**
 * Ed25519 public key implementation. Internal class — instances are obtained via
 * the public factory methods on {@link PublicKey}.
 */
public final class Ed25519PublicKey implements PublicKey {

    private final byte[] pub32;

    Ed25519PublicKey(@NonNull final byte[] pub32) {
        Objects.requireNonNull(pub32, "pub32 must not be null");
        if (pub32.length != 32) {
            throw new IllegalArgumentException("Ed25519 public key must be 32 bytes");
        }
        this.pub32 = pub32.clone();
    }

    @Override
    public boolean verify(@NonNull final byte[] message, @NonNull final byte[] signature) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
        return Ed25519.verify(signature, 0, pub32, 0, message, 0, message.length);
    }

    @Override
    @NonNull
    public byte[] toRawBytes() {
        return pub32.clone();
    }

    @Override
    @NonNull
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ED25519;
    }

    @Override
    @NonNull
    public KeyType type() {
        return KeyType.PUBLIC;
    }

    @Override
    @NonNull
    public byte[] toBytes(@NonNull final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (!container.supportsType(KeyType.PUBLIC)) {
            throw new IllegalArgumentException("Container does not support public keys: " + container);
        }
        if (container.getRawFormat() != RawFormat.BYTES) {
            throw new IllegalArgumentException("toBytes requires BYTES format: " + container);
        }
        try {
            if (container.getEncoding() == KeyEncoding.DER) {
                final SubjectPublicKeyInfo spki =
                        new SubjectPublicKeyInfo(new AlgorithmIdentifier(ID_ED25519), pub32);
                return spki.getEncoded("DER");
            }
            throw new IllegalArgumentException("Unsupported container/format for Ed25519 public key: " + container);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error encoding Ed25519 public key", e);
        }
    }

    @Override
    @NonNull
    public String toString(@NonNull final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (container.getRawFormat() != RawFormat.STRING) {
            throw new IllegalArgumentException("Requested String for non-STRING container: " + container);
        }
        if (container == KeyFormat.SPKI_WITH_PEM) {
            return PemUtil.toPem("PUBLIC KEY", toBytes(KeyFormat.SPKI_WITH_DER));
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
