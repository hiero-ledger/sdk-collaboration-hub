package org.hiero.keys;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A pair of public and private keys. Mirrors the meta-language type
 * {@code keys.KeyPair}. Both fields are {@code @@immutable}, so the type is a Java
 * record.
 *
 * @param publicKey  the public key
 * @param privateKey the private key
 */
public record KeyPair(@NonNull PublicKey publicKey, @NonNull PrivateKey privateKey) {

    public KeyPair {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
    }

    /**
     * Generates a new key pair for the given algorithm.
     *
     * @param algorithm the algorithm to generate the key pair for
     * @return the new key pair
     */
    @NonNull
    public static KeyPair generate(@NonNull final KeyAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        final PrivateKey privateKey = PrivateKey.generatePrivateKey(algorithm);
        return new KeyPair(privateKey.createPublicKey(), privateKey);
    }
}
