package org.hiero.keys;

import java.util.Objects;
import org.hiero.keys.impl.KeyFactoryImpl;
import org.hiero.keys.io.ByteImportEncoding;
import org.hiero.keys.io.KeyFormat;
import org.jspecify.annotations.NonNull;

/**
 * Public key abstraction. Mirrors the meta-language type {@code keys.PublicKey}.
 *
 * <p>The meta-language defines a number of namespace-level factory functions for
 * creating public keys. Following the api-best-practice rule "factories should be
 * implemented as static methods on the type that is created", they live here as
 * static methods on {@code PublicKey} and delegate to {@link KeyFactoryImpl}.
 */
public interface PublicKey extends Key {

    /**
     * Verifies a signature against this public key.
     *
     * @param message   the original message
     * @param signature the signature to verify
     * @return {@code true} if the signature is valid for the message and this key
     */
    boolean verify(@NonNull byte[] message, @NonNull byte[] signature);

    // ---- Factory methods (mapping of namespace-level functions) ----

    /**
     * Generates a new public key for the given algorithm. Internally a fresh
     * private key is generated and the corresponding public key is derived.
     */
    @NonNull
    static PublicKey generatePublicKey(@NonNull final KeyAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return KeyFactoryImpl.generatePrivateKey(algorithm).createPublicKey();
    }

    /**
     * Creates a {@link PublicKey} from raw bytes for a given algorithm.
     */
    @NonNull
    static PublicKey createPublicKey(@NonNull final KeyAlgorithm algorithm, @NonNull final byte[] rawBytes) {
        return KeyFactoryImpl.createPublicKey(algorithm, rawBytes);
    }

    /**
     * Creates a {@link PublicKey} from a string in the given byte-import encoding.
     */
    @NonNull
    static PublicKey createPublicKey(
            @NonNull final KeyAlgorithm algorithm,
            @NonNull final ByteImportEncoding encoding,
            @NonNull final String value) {
        return KeyFactoryImpl.createPublicKey(algorithm, encoding, value);
    }

    /**
     * Creates a {@link PublicKey} from a binary container value.
     */
    @NonNull
    static PublicKey createPublicKey(@NonNull final KeyFormat container, @NonNull final byte[] value) {
        return KeyFactoryImpl.createPublicKey(container, value);
    }

    /**
     * Creates a {@link PublicKey} from a string container value.
     */
    @NonNull
    static PublicKey createPublicKey(@NonNull final KeyFormat container, @NonNull final String value) {
        return KeyFactoryImpl.createPublicKey(container, value);
    }

    /**
     * Convenience: creates a {@link PublicKey} from a SPKI PEM string. This is the
     * Java mapping of the meta-language convenience factory
     * {@code createPublicKey(value: string)}.
     */
    @NonNull
    static PublicKey createPublicKey(@NonNull final String value) {
        return createPublicKey(KeyFormat.SPKI_WITH_PEM, value);
    }
}
