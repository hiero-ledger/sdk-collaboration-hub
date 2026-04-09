package org.hiero.keys.impl;

import static org.hiero.keys.impl.Oids.ID_EC_PUBLIC_KEY;
import static org.hiero.keys.impl.Oids.ID_SECP256K1;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.hiero.keys.Key;
import org.hiero.keys.KeyAlgorithm;
import org.hiero.keys.KeyType;
import org.hiero.keys.PublicKey;
import org.hiero.keys.io.KeyEncoding;
import org.hiero.keys.io.KeyFormat;
import org.hiero.keys.io.RawFormat;
import org.jspecify.annotations.NonNull;

/**
 * ECDSA secp256k1 public key implementation. Internal class.
 */
public final class EcdsaPublicKey implements PublicKey {

    private static final X9ECParameters CURVE;

    private static final ECDomainParameters DOMAIN;

    static {
        CURVE = SECNamedCurves.getByName("secp256k1");
        DOMAIN = new ECDomainParameters(CURVE.getCurve(), CURVE.getG(), CURVE.getN(), CURVE.getH());
    }

    private final byte[] qCompressed;

    EcdsaPublicKey(@NonNull final byte[] qBytes) {
        Objects.requireNonNull(qBytes, "qBytes must not be null");
        if (qBytes.length == 33) {
            this.qCompressed = qBytes.clone();
        } else if (qBytes.length == 65) {
            final ECPoint p = CURVE.getCurve().decodePoint(qBytes).normalize();
            this.qCompressed = p.getEncoded(true);
        } else {
            throw new IllegalArgumentException("ECDSA public key must be 33 or 65 bytes");
        }
    }

    @NonNull
    static EcdsaPublicKey fromSpkiDer(@NonNull final byte[] der) {
        try {
            final SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(der);
            if (!spki.getAlgorithm().getAlgorithm().equals(ID_EC_PUBLIC_KEY)) {
                throw new IllegalArgumentException("Not an EC public key");
            }
            final Object params = spki.getAlgorithm().getParameters();
            if (params == null || !params.equals(ID_SECP256K1)) {
                throw new IllegalArgumentException("Unsupported curve, expected secp256k1");
            }
            return new EcdsaPublicKey(spki.getPublicKeyData().getBytes());
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException("Invalid SPKI DER", e);
        }
    }

    @Override
    public boolean verify(@NonNull final byte[] message, @NonNull final byte[] signature) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
        if (signature.length != 64) {
            throw new IllegalArgumentException("ECDSA signature must be 64 bytes (r||s)");
        }
        final byte[] hash = EcdsaPrivateKey.keccak256(message);
        final ECDSASigner verifier = new ECDSASigner();
        final ECPoint Q = CURVE.getCurve().decodePoint(qCompressed).normalize();
        verifier.init(false, new ECPublicKeyParameters(Q, DOMAIN));
        final BigInteger r = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
        final BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));
        return verifier.verifySignature(hash, r, s);
    }

    @Override
    @NonNull
    public byte[] toRawBytes() {
        return qCompressed.clone();
    }

    @Override
    @NonNull
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ECDSA;
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
        if (container.getEncoding() != KeyEncoding.DER) {
            throw new IllegalArgumentException("Only DER supported for bytes here: " + container);
        }
        try {
            final AlgorithmIdentifier alg = new AlgorithmIdentifier(ID_EC_PUBLIC_KEY, ID_SECP256K1);
            return new SubjectPublicKeyInfo(alg, qCompressed).getEncoded("DER");
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error encoding ECDSA public key", e);
        }
    }

    @Override
    @NonNull
    public String toString(@NonNull final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (container.getRawFormat() != RawFormat.STRING) {
            throw new IllegalArgumentException("Requested String for non-STRING container: " + container);
        }
        if (container != KeyFormat.SPKI_WITH_PEM) {
            throw new IllegalArgumentException("Unsupported container for toString: " + container);
        }
        return PemUtil.toPem("PUBLIC KEY", toBytes(KeyFormat.SPKI_WITH_DER));
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
