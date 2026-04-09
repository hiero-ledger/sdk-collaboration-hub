package org.hiero.keys.impl;

import static org.hiero.keys.impl.Oids.ID_EC_PUBLIC_KEY;
import static org.hiero.keys.impl.Oids.ID_SECP256K1;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
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
 * ECDSA secp256k1 private key implementation. Internal class — instances are
 * obtained via the public factory methods on {@link PrivateKey}.
 */
public final class EcdsaPrivateKey implements PrivateKey {

    private static final X9ECParameters CURVE;

    private static final ECDomainParameters DOMAIN;

    private static final ThreadLocal<SecureRandom> RANDOM = ThreadLocal.withInitial(SecureRandom::new);

    static {
        CURVE = SECNamedCurves.getByName("secp256k1");
        DOMAIN = new ECDomainParameters(CURVE.getCurve(), CURVE.getG(), CURVE.getN(), CURVE.getH());
    }

    private final BigInteger d;

    EcdsaPrivateKey(@NonNull final BigInteger d) {
        this.d = Objects.requireNonNull(d, "d must not be null");
    }

    @NonNull
    static EcdsaPrivateKey generate() {
        final SecureRandom rnd = RANDOM.get();
        BigInteger x;
        do {
            x = new BigInteger(CURVE.getN().bitLength(), rnd);
        } while (x.signum() <= 0 || x.compareTo(CURVE.getN()) >= 0);
        return new EcdsaPrivateKey(x);
    }

    @NonNull
    static EcdsaPrivateKey fromRaw(@NonNull final byte[] raw32) {
        Objects.requireNonNull(raw32, "raw32 must not be null");
        if (raw32.length != 32) {
            throw new IllegalArgumentException("ECDSA private key must be 32 bytes");
        }
        return new EcdsaPrivateKey(new BigInteger(1, raw32));
    }

    @NonNull
    static EcdsaPrivateKey fromPkcs8Der(@NonNull final byte[] der) {
        try {
            final PrivateKeyInfo pki = PrivateKeyInfo.getInstance(der);
            if (!pki.getPrivateKeyAlgorithm().getAlgorithm().equals(ID_EC_PUBLIC_KEY)) {
                throw new IllegalArgumentException("Not an EC private key");
            }
            final Object params = pki.getPrivateKeyAlgorithm().getParameters();
            if (params == null || !params.equals(ID_SECP256K1)) {
                throw new IllegalArgumentException("Unsupported curve, expected secp256k1");
            }
            final ECPrivateKey ec = ECPrivateKey.getInstance(pki.parsePrivateKey());
            return new EcdsaPrivateKey(ec.getKey());
        } catch (final IOException e) {
            throw new IllegalArgumentException("Invalid PKCS#8 DER", e);
        }
    }

    @Override
    @NonNull
    public byte[] sign(@NonNull final byte[] message) {
        Objects.requireNonNull(message, "message must not be null");
        final byte[] hash = keccak256(message);
        final ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(d, DOMAIN));
        final BigInteger[] rs = signer.generateSignature(hash);
        final byte[] out = new byte[64];
        toFixed(rs[0], out, 0);
        toFixed(rs[1], out, 32);
        return out;
    }

    @Override
    @NonNull
    public PublicKey createPublicKey() {
        final ECPoint Q = DOMAIN.getG().multiply(d).normalize();
        final byte[] comp = Q.getEncoded(true);
        return new EcdsaPublicKey(comp);
    }

    @Override
    @NonNull
    public byte[] toRawBytes() {
        final byte[] out = new byte[32];
        toFixed(d, out, 0);
        return out;
    }

    @Override
    @NonNull
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.ECDSA;
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
        if (container.getEncoding() != KeyEncoding.DER) {
            throw new IllegalArgumentException("Only DER supported for bytes here: " + container);
        }
        try {
            final ECPoint Q = DOMAIN.getG().multiply(d).normalize();
            final ECPrivateKey ecPrivate = new ECPrivateKey(
                    256, d, new DERBitString(Q.getEncoded(true)), new X962Parameters(ID_SECP256K1));
            final AlgorithmIdentifier alg = new AlgorithmIdentifier(ID_EC_PUBLIC_KEY, ID_SECP256K1);
            return new PrivateKeyInfo(alg, ecPrivate).getEncoded("DER");
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error encoding ECDSA private key", e);
        }
    }

    @Override
    @NonNull
    public String toString(@NonNull final KeyFormat container) {
        Objects.requireNonNull(container, "container must not be null");
        if (container.getRawFormat() != RawFormat.STRING) {
            throw new IllegalArgumentException("Requested String for non-STRING container: " + container);
        }
        if (container != KeyFormat.PKCS8_WITH_PEM) {
            throw new IllegalArgumentException("Unsupported container for toString: " + container);
        }
        return PemUtil.toPem("PRIVATE KEY", toBytes(KeyFormat.PKCS8_WITH_DER));
    }

    static byte[] keccak256(@NonNull final byte[] msg) {
        final KeccakDigest d = new KeccakDigest(256);
        d.update(msg, 0, msg.length);
        final byte[] out = new byte[32];
        d.doFinal(out, 0);
        return out;
    }

    private static void toFixed(final BigInteger bi, final byte[] out, final int off) {
        final byte[] tmp = bi.toByteArray();
        Arrays.fill(out, off, off + 32, (byte) 0);
        final int copy = Math.min(32, tmp.length);
        System.arraycopy(tmp, tmp.length - copy, out, off + 32 - copy, copy);
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
