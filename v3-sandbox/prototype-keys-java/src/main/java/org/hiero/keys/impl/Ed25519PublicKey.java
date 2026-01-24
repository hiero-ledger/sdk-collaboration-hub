package org.hiero.keys.impl;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.hiero.keys.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static org.hiero.keys.impl.Oids.ID_ED25519;

public final class Ed25519PublicKey implements PublicKey {
    private final byte[] pub32;

    public Ed25519PublicKey(final byte[] pub32) {
        this.pub32 = Objects.requireNonNull(pub32, "pub32 must not be null");
        if (pub32.length != 32) {
            throw new IllegalArgumentException("Ed25519 public key must be 32 bytes");
        }
    }

    @Override
    public boolean verify(final byte[] message, final byte[] signature) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
        return Ed25519.verify(signature, 0, pub32, 0, message, 0, message.length);
    }

    @Override
    public byte[] toRawBytes() {
        return Arrays.copyOf(pub32, pub32.length);
    }

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ED25519;
    }

    @Override
    public KeyType type() {
        return KeyType.PUBLIC;
    }

    @Override
    public byte[] toBytes(final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (!container.supportsType(KeyType.PUBLIC)) {
            throw new IllegalArgumentException("Container does not support public keys: " + container);
        }
        if (container.encoding().getFormat() != RawFormat.BYTES) {
            throw new IllegalArgumentException("toBytes requires BYTES format: " + container);
        }
        try {
            if (container.encoding() == KeyEncoding.DER) {
                final SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(new AlgorithmIdentifier(ID_ED25519), pub32);
                return spki.getEncoded("DER");
            }
            throw new IllegalArgumentException("Unsupported container/format for Ed25519 public key: " + container);
        } catch (IOException e) {
            throw new RuntimeException("Error encoding Ed25519 public key", e);
        }
    }

    @Override
    public String toString(final KeyFormat container) {
        if (container.encoding().getFormat() != RawFormat.STRING) {
            throw new IllegalArgumentException("Requested String for non-STRING container: " + container);
        }
        if (container == KeyFormat.SPKI_WITH_PEM) {
            return PemUtil.toPem("PUBLIC KEY", toBytes(KeyFormat.SPKI_WITH_DER));
        }
        throw new IllegalArgumentException("Unsupported container for toString: " + container);
    }
}
