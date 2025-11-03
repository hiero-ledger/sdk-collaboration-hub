package org.hiero.sdk.simple.internal.network.key;

import java.io.IOException;
import java.util.Objects;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

/**
 * Internal utilities to create and convert key representations.
 * <p>
 * Although placed in an internal package, methods are public to support factory-style helpers used by the
 * public API. They are not intended for direct consumption by SDK users.
 */
public final class KeyFactory {

    private KeyFactory() {
    }

    /**
     * Creates a private key from its binary representation for the specified algorithm.
     *
     * @param privateKey the encoded private key bytes
     * @param algorithm  the key algorithm the bytes belong to
     * @return the private key
     * @throws IllegalArgumentException if the bytes do not match the algorithm
     */
    @NonNull
    public static PrivateKey createPrivateKey(@NonNull final KeyAlgorithm algorithm, @NonNull final byte[] privateKey) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        if (algorithm == KeyAlgorithm.ED25519) {
            if ((privateKey.length == Ed25519.SECRET_KEY_SIZE)
                    || (privateKey.length == Ed25519.SECRET_KEY_SIZE + Ed25519.PUBLIC_KEY_SIZE)) {
                return KeyUtilitiesED25519.createPrivateKeyFromBytes(privateKey);
            }
            final PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey);
            if (privateKeyInfo.getPrivateKeyAlgorithm()
                    .equals(new AlgorithmIdentifier(KeyUtilitiesED25519.ID_ED25519))) {
                try {
                    return KeyUtilitiesED25519.createPrivateKeyFromPrivateKeyInfo(privateKeyInfo);
                } catch (IOException e) {
                    throw new RuntimeException("Error creating key with algorithm " + algorithm, e);
                }
            }
        }
        if (algorithm == KeyAlgorithm.ECDSA) {
            final PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey);
            return KeyUtilitiesECDSA.createPrivateKeyFromPrivateKeyInfo(privateKeyInfo);
        }
        throw new IllegalArgumentException(
                "The provided private key does not match the provided algorithm: " + algorithm);
    }

    /**
     * Creates a private key from a Java Security {@link java.security.PrivateKey}.
     *
     * @param privateKey the Java Security private key
     * @return the corresponding Hiero private key
     * @throws IllegalArgumentException if the given key cannot be encoded
     */
    @NonNull
    public static PrivateKey createPrivateKey(final java.security.@NonNull PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        final byte[] encoded = privateKey.getEncoded();
        if (encoded == null) {
            throw new IllegalArgumentException("Given key does not support encoding");
        }
        final KeyAlgorithm algorithm = KeyAlgorithm.valueOf(privateKey.getAlgorithm());
        return createPrivateKey(algorithm, encoded);
    }

    /**
     * Creates a public key from its binary representation.
     *
     * @param publicKey the encoded public key bytes
     * @return the public key
     * @throws UnsupportedOperationException until implemented
     */
    @NonNull
    public static PublicKey createPublicKey(KeyAlgorithm algorithm, @NonNull final byte[] publicKey) {
        if(algorithm == KeyAlgorithm.ECDSA) {
            return new PublicKeyWithECDSA(publicKey);
        }
        if(algorithm == KeyAlgorithm.ED25519) {
            return new PublicKeyWithED25519(publicKey);
        }
        throw new UnsupportedOperationException("Public key creation for algorithm " + algorithm + " is not supported");
    }

    /**
     * Creates a public key from a Java Security {@link java.security.PublicKey}.
     *
     * @param publicKey the Java Security public key
     * @return the corresponding Hiero public key
     * @throws IllegalArgumentException if the given key cannot be encoded
     */
    public static PublicKey createPublicKey(final java.security.@NonNull PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        final byte[] encoded = publicKey.getEncoded();
        if (encoded == null) {
            throw new IllegalArgumentException("Given key does not support encoding");
        }
        final KeyAlgorithm algorithm = KeyAlgorithm.valueOf(publicKey.getAlgorithm());
        return createPublicKey(algorithm, encoded);
    }

    /**
     * Decodes a hexadecimal string to a byte array.
     *
     * @param hex the hex string (with or without 0x prefix)
     * @return the decoded bytes
     */
    @NonNull
    private static byte[] decodeHex(@NonNull final String hex) {
        Objects.requireNonNull(hex, "hex must not be null");
        return Hex.decode(hex.startsWith("0x") ? hex.substring(2) : hex);
    }

    /**
     * Creates a Java Security {@link java.security.PrivateKey} view backed by the given Hiero {@link PrivateKey}.
     *
     * @param privateKey the Hiero private key
     * @param encoding   the encoding/format to use
     * @return a Java Security PrivateKey view backed by the given key
     */
    public static java.security.@NonNull PrivateKey toJavaPrivateKey(@NonNull PrivateKey privateKey,
            @NonNull KeyEncoding encoding) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(encoding, "encoding must not be null");
        return new java.security.PrivateKey() {
            @Override
            public String getAlgorithm() {
                return privateKey.algorithm().name();
            }

            @Override
            public String getFormat() {
                return encoding.name();
            }

            @Override
            public byte[] getEncoded() {
                return privateKey.toBytes(encoding);
            }

            @Override
            public boolean equals(Object obj) {
                return privateKey.equals(obj);
            }

            @Override
            public int hashCode() {
                return privateKey.hashCode();
            }
        };
    }

    /**
     * Creates a Java Security {@link java.security.PublicKey} view backed by the given Hiero {@link PublicKey}.
     *
     * @param publicKey the Hiero public key
     * @param encoding  the encoding/format to use
     * @return a Java Security PublicKey view backed by the given key
     */
    public static java.security.@NonNull PublicKey toJavaPublicKey(@NonNull PublicKey publicKey,
            @NonNull KeyEncoding encoding) {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(encoding, "encoding must not be null");
        return new java.security.PublicKey() {
            @Override
            public String getAlgorithm() {
                return publicKey.algorithm().name();
            }

            @Override
            public String getFormat() {
                return encoding.name();
            }

            @Override
            public byte[] getEncoded() {
                return publicKey.toBytes(encoding);
            }

            @Override
            public boolean equals(Object obj) {
                return publicKey.equals(obj);
            }

            @Override
            public int hashCode() {
                return publicKey.hashCode();
            }
        };
    }

    /**
     * Creates a new {@link PrivateKey} instance for the specified {@link KeyAlgorithm}.
     * <p>
     * Supported algorithms are ECDSA and ED25519. If an unsupported algorithm is provided,
     * an {@link UnsupportedOperationException} is thrown.
     *
     * @param algorithm the key algorithm for which to create a private key
     * @return a new {@link PrivateKey} instance for the specified algorithm
     * @throws UnsupportedOperationException if the algorithm is not supported
     */
    public static @NonNull PrivateKey createPrivateKey(KeyAlgorithm algorithm) {
        if(algorithm == KeyAlgorithm.ECDSA) {
            return KeyUtilitiesECDSA.createPrivateKey();
        }
        if(algorithm == KeyAlgorithm.ED25519) {
            return KeyUtilitiesED25519.createPrivateKey();
        } else {
            throw new UnsupportedOperationException("Private key creation for algorithm " + algorithm + " is not supported");
        }
    }

    public static byte[] decode(String key, KeyEncoding encoding) {
        return encoding == KeyEncoding.DER ? Hex.decode(key) : Hex.decode(key.startsWith("0x") ? key.substring(2) : key);
    }
}
