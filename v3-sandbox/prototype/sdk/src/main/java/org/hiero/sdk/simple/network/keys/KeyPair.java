package org.hiero.sdk.simple.network.keys;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Immutable tuple holding a {@link PrivateKey} and its corresponding {@link PublicKey}.
 *
 * @param privateKey the private key
 * @param publicKey  the corresponding public key
 */
public record KeyPair(@NonNull PrivateKey privateKey, @NonNull PublicKey publicKey) {

    /**
     * Validates that both record components are non-null.
     */
    public KeyPair {
        Objects.requireNonNull(privateKey, "Private key cannot be null");
        Objects.requireNonNull(publicKey, "Public key cannot be null");
    }

    /**
     * Creates a {@code KeyPair} from an existing {@link PrivateKey} by deriving the matching public key.
     *
     * @param privateKey the private key
     * @return a key pair consisting of {@code privateKey} and the derived public key
     */
    @NonNull
    public static KeyPair of(@NonNull final PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "Private key cannot be null");
        return new KeyPair(privateKey, privateKey.createPublicKey());
    }

    /**
     * Generates a fresh {@code KeyPair} for the given algorithm.
     *
     * @param algorithm the key algorithm
     * @return a new key pair
     */
    @NonNull
    public static KeyPair generate(@NonNull final KeyAlgorithm algorithm) {
        return of(PrivateKey.generate(algorithm));
    }
}
