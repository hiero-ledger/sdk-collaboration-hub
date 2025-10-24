package org.hiero.sdk.simple.internal.network.key;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

public final class KeyUtilitiesED25519 {
    static final ASN1ObjectIdentifier ID_ED25519 = new ASN1ObjectIdentifier("1.3.101.112");
    static final ASN1ObjectIdentifier ID_ECDSA_SECP256K1 = new ASN1ObjectIdentifier("1.3.132.0.10");
    static final ASN1ObjectIdentifier ID_EC_PUBLIC_KEY = new ASN1ObjectIdentifier("1.2.840.10045.2.1");
    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    @NonNull
    public static PublicKey createPublicKey(@NonNull final PrivateKeyWithED25519 privateKey) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        final byte[] publicKeyData = new byte[Ed25519.PUBLIC_KEY_SIZE];
        Ed25519.generatePublicKey(privateKey.keyData(), 0, publicKeyData, 0);
        if (publicKeyData.length == Ed25519.PUBLIC_KEY_SIZE) {
            // Validate the key if it's not all zero public key, see HIP-540
            if (!Arrays.equals(publicKeyData, new byte[32])) {
                final Ed25519.PublicPoint publicPoint = Ed25519.validatePublicKeyPartialExport(publicKeyData, 0);
                if (publicPoint == null) {
                    throw new IllegalArgumentException("invalid public key");
                }
            }
            // If this is a 32 byte string, assume an Ed25519 public key
            return new PublicKeyWithED25519(publicKeyData);
        }
        // Assume a DER-encoded public key descriptor
        final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKeyData);
        return new PublicKeyWithED25519(subjectPublicKeyInfo.getPublicKeyData().getBytes());
    }

    @NonNull
    public static byte[] sign(@NonNull final PrivateKeyWithED25519 privateKey, @NonNull final byte[] message) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(message, "message must not be null");
        final byte[] signature = new byte[Ed25519.SIGNATURE_SIZE];
        Ed25519.sign(privateKey.keyData(), 0, message, 0, message.length, signature, 0);
        return signature;
    }

    @NonNull
    public static byte[] toBytes(@NonNull final PrivateKeyWithED25519 privateKey, @NonNull final KeyEncoding encoding) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(encoding, "encoding must not be null");
        if (encoding == KeyEncoding.DER) {
            try {
                return new PrivateKeyInfo(new AlgorithmIdentifier(KeyUtilitiesED25519.ID_ED25519),
                        new DEROctetString(privateKey.keyData()))
                        .getEncoded("DER");
            } catch (IOException e) {
                throw new RuntimeException("Error creating DER byte representation", e);
            }
        }
        if (encoding == KeyEncoding.RAW) {
            return Arrays.copyOf(privateKey.keyData(), privateKey.keyData().length);
        }
        throw new IllegalArgumentException("Unsupported key encoding: " + encoding);
    }

    public static boolean verify(@NonNull final PublicKeyWithED25519 publicKey, @NonNull final byte[] message,
            @NonNull final byte[] signature) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
        return Ed25519.verify(signature, 0, publicKey.keyData(), 0, message, 0, message.length);
    }

    public static @NonNull byte[] toBytes(@NonNull final PublicKeyWithED25519 publicKey,
            @NonNull final KeyEncoding encoding) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(encoding, "encoding must not be null");
        if (encoding == KeyEncoding.DER) {
            try {
                return new SubjectPublicKeyInfo(new AlgorithmIdentifier(KeyUtilitiesED25519.ID_ED25519),
                        publicKey.keyData()).getEncoded(
                        "DER");
            } catch (IOException e) {
                throw new RuntimeException("Error creating DER byte representation", e);
            }
        }
        if (encoding == KeyEncoding.RAW) {
            return Arrays.copyOf(publicKey.keyData(), publicKey.keyData().length);
        }
        throw new IllegalArgumentException("Unsupported key encoding: " + encoding);
    }

    @NonNull
    public static PrivateKey createPrivateKey() {
        // extra 32 bytes for chain code
        final byte[] data = new byte[Ed25519.SECRET_KEY_SIZE + 32];
        secureRandom.get().nextBytes(data);
        final byte[] keyData = Arrays.copyOfRange(data, 0, 32);
        final KeyParameter chainCode = new KeyParameter(data, 32, 32);
        return new PrivateKeyWithED25519(keyData, chainCode);
    }

    @NonNull
    public static PrivateKey createPrivateKeyFromBytes(@NonNull final byte[] privateKey) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        if ((privateKey.length != Ed25519.SECRET_KEY_SIZE)
                && (privateKey.length != Ed25519.SECRET_KEY_SIZE + Ed25519.PUBLIC_KEY_SIZE)) {
            throw new IllegalArgumentException(
                    "Invalid private key length: " + privateKey.length + ". Expected 32 or 64 bytes.");
        }
        return new PrivateKeyWithED25519(Arrays.copyOfRange(privateKey, 0, Ed25519.SECRET_KEY_SIZE), null);
    }

    @NonNull
    public static PrivateKey createPrivateKeyFromPrivateKeyInfo(@NonNull final PrivateKeyInfo privateKeyInfo)
            throws IOException {
        Objects.requireNonNull(privateKeyInfo, "privateKeyInfo must not be null");
        if (!privateKeyInfo.getPrivateKeyAlgorithm().equals(new AlgorithmIdentifier(KeyUtilitiesED25519.ID_ED25519))) {
            throw new IllegalArgumentException(
                    "Unsupported private key algorithm: " + privateKeyInfo.getPrivateKeyAlgorithm());
        }
        var privateKey = (ASN1OctetString) privateKeyInfo.parsePrivateKey();
        return new PrivateKeyWithED25519(privateKey.getOctets(), null);
    }
}
