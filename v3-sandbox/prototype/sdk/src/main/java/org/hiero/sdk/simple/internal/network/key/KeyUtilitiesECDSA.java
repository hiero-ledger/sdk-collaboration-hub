package org.hiero.sdk.simple.internal.network.key;

import static org.hiero.sdk.simple.internal.network.key.KeyUtilitiesED25519.ID_ECDSA_SECP256K1;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

public final class KeyUtilitiesECDSA {

    static final X9ECParameters ECDSA_SECP256K1_CURVE = SECNamedCurves.getByName("secp256k1");
    static final ECDomainParameters ECDSA_SECP256K1_DOMAIN = new ECDomainParameters(
            ECDSA_SECP256K1_CURVE.getCurve(),
            ECDSA_SECP256K1_CURVE.getG(),
            ECDSA_SECP256K1_CURVE.getN(),
            ECDSA_SECP256K1_CURVE.getH());
    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    private KeyUtilitiesECDSA() {
    }

    @NonNull
    public static PublicKey createPublicKey(@NonNull final PrivateKeyWithECDSA privateKey) {
        final ECPoint q = ECDSA_SECP256K1_DOMAIN.getG().multiply(privateKey.keyData());
        final ECPublicKeyParameters publicParams = new ECPublicKeyParameters(q, ECDSA_SECP256K1_DOMAIN);
        return createPublicKey(publicParams.getQ().getEncoded(true));
    }

    @NonNull
    private static PublicKey createPublicKey(@NonNull final byte[] publicKey) {
        // Validate the key if it's not all zero public key, see HIP-540
        if (java.util.Arrays.equals(publicKey, new byte[33])) {
            return new PublicKeyWithECDSA(publicKey);
        }
        if (publicKey.length == 33 || publicKey.length == 65) {
            return new PublicKeyWithECDSA(
                    // compress and validate the key
                    ECDSA_SECP256K1_CURVE.getCurve().decodePoint(publicKey).getEncoded(true));
        }

        // Assume a DER-encoded public key descriptor
        final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey);
        return createPublicKey(subjectPublicKeyInfo.getPublicKeyData().getBytes());
    }

    @NonNull
    public static PrivateKey createPrivateKey() {
        final ECKeyPairGenerator generator = new ECKeyPairGenerator();
        final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ECDSA_SECP256K1_DOMAIN,
                secureRandom.get());
        generator.init(keygenParams);
        final AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
        final ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
        return new PrivateKeyWithECDSA(privParams.getD());
    }

    @NonNull
    public static PrivateKey createPrivateKeyFromPrivateKeyInfo(@NonNull final PrivateKeyInfo privateKeyInfo) {
        try {
            final ECPrivateKey privateKey = ECPrivateKey.getInstance(privateKeyInfo.parsePrivateKey());
            return new PrivateKeyWithECDSA(privateKey.getKey());
        } catch (IllegalArgumentException e) {
            // Try legacy import
            try {
                final ASN1OctetString privateKey = (ASN1OctetString) privateKeyInfo.parsePrivateKey();
                return new PrivateKeyWithECDSA(new BigInteger(1, privateKey.getOctets()));
            } catch (IOException ex) {
                throw new RuntimeException("Error creating private key", ex);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating private key", e);
        }
    }

    @NonNull
    public static byte[] sign(@NonNull final PrivateKeyWithECDSA privateKey, @NonNull final byte[] message) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(message, "message must not be null");
        final byte[] hash = KeyUtilitiesECDSA.calcKeccak256(message);
        final ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(privateKey.keyData(), ECDSA_SECP256K1_DOMAIN));
        final BigInteger[] bigSig = signer.generateSignature(hash);
        final byte[] sigBytes = Arrays.copyOf(bigIntTo32Bytes(bigSig[0]), 64);
        System.arraycopy(bigIntTo32Bytes(bigSig[1]), 0, sigBytes, 32, 32);
        return sigBytes;
    }

    @NonNull
    public static byte[] toBytes(@NonNull final PrivateKeyWithECDSA privateKey, @NonNull final KeyEncoding encoding) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(encoding, "encoding must not be null");
        if (encoding == KeyEncoding.DER) {
            try {
                return new ECPrivateKey(
                        256,
                        privateKey.keyData(),
                        new DERBitString(privateKey.createPublicKey().toBytes(KeyEncoding.RAW)),
                        new X962Parameters(ID_ECDSA_SECP256K1))
                        .getEncoded("DER");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (encoding == KeyEncoding.RAW) {
            return bigIntTo32Bytes(privateKey.keyData());
        }
        throw new IllegalArgumentException("Unsupported key encoding: " + encoding);
    }

    public static boolean verify(@NonNull final PublicKeyWithECDSA publicKey, @NonNull final byte[] message,
            @NonNull final byte[] signature) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
        final byte[] hash = KeyUtilitiesECDSA.calcKeccak256(message);
        final ECDSASigner signer = new ECDSASigner();
        signer.init(
                false,
                new ECPublicKeyParameters(
                        KeyUtilitiesECDSA.ECDSA_SECP256K1_CURVE.getCurve().decodePoint(publicKey.keyData()),
                        KeyUtilitiesECDSA.ECDSA_SECP256K1_DOMAIN));
        final BigInteger r = new BigInteger(1, java.util.Arrays.copyOf(signature, 32));
        final BigInteger s = new BigInteger(1, java.util.Arrays.copyOfRange(signature, 32, 64));
        return signer.verifySignature(hash, r, s);
    }

    @NonNull
    public static byte[] toBytes(@NonNull final PublicKeyWithECDSA publicKey, @NonNull final KeyEncoding encoding) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(encoding, "encoding must not be null");
        if (encoding == KeyEncoding.DER) {
            try {
                return new SubjectPublicKeyInfo(
                        new AlgorithmIdentifier(KeyUtilitiesED25519.ID_EC_PUBLIC_KEY,
                                KeyUtilitiesED25519.ID_ECDSA_SECP256K1),
                        publicKey.keyData())
                        .getEncoded("DER");
            } catch (IOException e) {
                throw new RuntimeException("Error converting to DER", e);
            }
        }
        if (encoding == KeyEncoding.RAW) {
            return java.util.Arrays.copyOf(publicKey.keyData(), publicKey.keyData().length);
        }
        throw new IllegalArgumentException("Unsupported key encoding: " + encoding);
    }

    private static byte[] calcKeccak256(byte[] message) {
        var digest = new Keccak.Digest256();
        digest.update(message);
        return digest.digest();
    }

    private static byte[] bigIntTo32Bytes(BigInteger n) {
        byte[] bytes = n.toByteArray();
        byte[] bytes32 = new byte[32];
        System.arraycopy(
                bytes,
                Math.max(0, bytes.length - 32),
                bytes32,
                Math.max(0, 32 - bytes.length),
                Math.min(32, bytes.length));
        return bytes32;
    }

}
